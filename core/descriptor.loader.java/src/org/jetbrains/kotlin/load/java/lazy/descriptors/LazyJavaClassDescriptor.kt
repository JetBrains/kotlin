/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.FakePureImplementationsProvider
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.childForClassOrPackage
import org.jetbrains.kotlin.load.java.lazy.replaceComponents
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.platform.createMappedTypeParametersSubstitution
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.scopes.InnerClassesScopeWrapper
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

class LazyJavaClassDescriptor(
        outerContext: LazyJavaResolverContext,
        containingDeclaration: DeclarationDescriptor,
        private val jClass: JavaClass,
        private val additionalSupertypeClassDescriptor: ClassDescriptor? = null
) : ClassDescriptorBase(outerContext.storageManager, containingDeclaration, jClass.name,
                        outerContext.components.sourceElementFactory.source(jClass),
                        /* isExternal = */ false), JavaClassDescriptor {

    companion object {
        @JvmStatic
        private val PUBLIC_METHOD_NAMES_IN_OBJECT = setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")
    }

    private val c: LazyJavaResolverContext = outerContext.childForClassOrPackage(this, jClass)

    init {
        c.components.javaResolverCache.recordClass(jClass, this)

        assert(jClass.lightClassOriginKind == null) {
            "Creating LazyJavaClassDescriptor for light class $jClass"
        }
    }

    private val kind = when {
        jClass.isAnnotationType -> ClassKind.ANNOTATION_CLASS
        jClass.isInterface -> ClassKind.INTERFACE
        jClass.isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

    private val modality = if (jClass.isAnnotationType)
                               Modality.FINAL
                           else Modality.convertFromFlags(jClass.isAbstract || jClass.isInterface, !jClass.isFinal)

    private val visibility = jClass.visibility
    private val isInner = jClass.outerClass != null && !jClass.isStatic

    override fun getKind() = kind
    override fun getModality() = modality

    // To workaround a problem with Scala compatibility (KT-9700),
    // we consider private visibility of a Java top level class as package private
    // Shortly: Scala plugin introduces special kind of "private in package" classes
    // which can be inherited from the same package.
    // Kotlin considers this "private in package" just as "private" and thinks they are invisible for inheritors,
    // so their functions are invisible fake which is not true.
    override fun getVisibility() =
            if (visibility == Visibilities.PRIVATE && jClass.outerClass == null) JavaVisibilities.PACKAGE_VISIBILITY else visibility

    override fun isInner() = isInner
    override fun isData() = false
    override fun isCompanionObject() = false
    override fun isHeader() = false
    override fun isImpl() = false

    private val typeConstructor = LazyJavaClassTypeConstructor()
    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    private val unsubstitutedMemberScope = LazyJavaClassMemberScope(c, this, jClass)
    override fun getUnsubstitutedMemberScope() = unsubstitutedMemberScope

    private val innerClassesScope = InnerClassesScopeWrapper(getUnsubstitutedMemberScope())
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = innerClassesScope

    private val staticScope = LazyJavaStaticClassScope(c, jClass, this)
    override fun getStaticScope(): MemberScope = staticScope

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null

    override fun getConstructors() = unsubstitutedMemberScope.constructors()

    override val annotations = c.resolveAnnotations(jClass)

    private val declaredParameters = c.storageManager.createLazyValue {
        jClass.typeParameters.map {
            p ->
            c.typeParameterResolver.resolveTypeParameter(p)
                ?: throw AssertionError("Parameter $p surely belongs to class $jClass, so it must be resolved")
        }
    }

    override fun getDeclaredTypeParameters() = declaredParameters()

    override fun getFunctionTypeForSamInterface(): SimpleType? = c.components.samConversionResolver.resolveFunctionTypeIfSamInterface(this)

    override fun isDefinitelyNotSamInterface(): Boolean {
        if (kind != ClassKind.INTERFACE) return true

        val candidates = jClass.methods.filter { it.isAbstract && it.typeParameters.isEmpty() }
        // From the definition of function interfaces in the Java specification (pt. 9.8):
        // "methods that are members of I that do not have the same signature as any public instance method of the class Object"
        // It means that if an interface declares `int hashCode()` then the method won't be taken into account when
        // checking if the interface is SAM.
        // We make here a conservative check just filtering out methods by name.
        // If we ignore a method with wrong signature (different from one in Object) it's not very bad,
        // we'll just say that the interface MAY BE a SAM when it's not and then more detailed check will be applied.
        if (candidates.count { it.name.identifier !in PUBLIC_METHOD_NAMES_IN_OBJECT } > 1) return true

        // Check if any of the super-interfaces contain too many methods to be a SAM
        return typeConstructor.supertypes.any {
            it.constructor.declarationDescriptor.safeAs<LazyJavaClassDescriptor>()?.isDefinitelyNotSamInterface == true
        }
    }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

    override fun toString() = "Lazy Java class ${this.fqNameUnsafe}"

    private inner class LazyJavaClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val parameters = c.storageManager.createLazyValue {
            this@LazyJavaClassDescriptor.computeConstructorTypeParameters()
        }

        override fun getParameters(): List<TypeParameterDescriptor> = parameters()

        override fun computeSupertypes(): Collection<KotlinType> {
            val javaTypes = jClass.supertypes
            val result = ArrayList<KotlinType>(javaTypes.size)
            val incomplete = ArrayList<JavaType>(0)

            val purelyImplementedSupertype: KotlinType? = getPurelyImplementedSupertype()

            for (javaType in javaTypes) {
                val kotlinType = c.typeResolver.transformJavaType(javaType, TypeUsage.SUPERTYPE.toAttributes())
                if (kotlinType.constructor.declarationDescriptor is NotFoundClasses.MockClassDescriptor) {
                    incomplete.add(javaType)
                }

                if (kotlinType.constructor == purelyImplementedSupertype?.constructor) {
                    continue
                }

                if (!KotlinBuiltIns.isAnyOrNullableAny(kotlinType)) {
                    result.add(kotlinType)
                }
            }

            // Add fake supertype kotlin.collection.Collection<E> to java.util.Collection<E> class if needed
            // Only needed when calculating built-ins member scope
            result.addIfNotNull(
                    additionalSupertypeClassDescriptor?.let {
                        createMappedTypeParametersSubstitution(it, this@LazyJavaClassDescriptor)
                                .buildSubstitutor().substitute(it.defaultType, Variance.INVARIANT)
                    })

            result.addIfNotNull(purelyImplementedSupertype)

            if (incomplete.isNotEmpty()) {
                c.components.errorReporter.reportIncompleteHierarchy(declarationDescriptor, incomplete.map { javaType ->
                    (javaType as JavaClassifierType).presentableText
                })
            }

            return if (result.isNotEmpty()) result.toList() else listOf(c.module.builtIns.anyType)
        }

        private fun getPurelyImplementedSupertype(): KotlinType? {
            val annotatedPurelyImplementedFqName = getPurelyImplementsFqNameFromAnnotation()?.takeIf { fqName ->
                !fqName.isRoot && fqName.startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)
            }

            val purelyImplementedFqName =
                    annotatedPurelyImplementedFqName
                    ?: FakePureImplementationsProvider.getPurelyImplementedInterface(fqNameSafe)
                    ?: return null

            val classDescriptor = c.module.resolveTopLevelClass(purelyImplementedFqName, NoLookupLocation.FROM_JAVA_LOADER) ?: return null

            val supertypeParameterCount = classDescriptor.typeConstructor.parameters.size
            val typeParameters = getTypeConstructor().parameters
            val typeParameterCount = typeParameters.size

            val parametersAsTypeProjections = when {
                typeParameterCount == supertypeParameterCount ->
                    typeParameters.map {
                        parameter ->
                        TypeProjectionImpl(Variance.INVARIANT, parameter.defaultType)
                    }
                typeParameterCount == 1 && supertypeParameterCount > 1 && annotatedPurelyImplementedFqName == null ->
                {
                    val parameter = TypeProjectionImpl(Variance.INVARIANT, typeParameters.single().defaultType)
                    (1..supertypeParameterCount).map { parameter } // TODO: List(supertypeParameterCount) { parameter }
                }
                else -> return null
            }

            return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, classDescriptor, parametersAsTypeProjections)
        }

        private fun getPurelyImplementsFqNameFromAnnotation(): FqName? {
            val annotation =
                    this@LazyJavaClassDescriptor.annotations.findAnnotation(JvmAnnotationNames.PURELY_IMPLEMENTS_ANNOTATION)
                    ?: return null

            val fqNameString = (annotation.allValueArguments.values.singleOrNull() as? StringValue)?.value ?: return null
            if (!isValidJavaFqName(fqNameString)) return null

            return FqName(fqNameString)
        }

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = c.components.supertypeLoopChecker

        override fun isFinal(): Boolean = isFinalClass

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@LazyJavaClassDescriptor

        override fun toString(): String = name.asString()
    }

    // Only needed when calculating built-ins member scope
    internal fun copy(
            javaResolverCache: JavaResolverCache, additionalSupertypeClassDescriptor: ClassDescriptor?
    ) = LazyJavaClassDescriptor(
            c.replaceComponents(c.components.replace(javaResolverCache = javaResolverCache)),
            containingDeclaration, jClass, additionalSupertypeClassDescriptor)
}
