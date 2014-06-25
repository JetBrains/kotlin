/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.java.lazy.child
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.resolve.java.lazy.resolveAnnotations
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.utils.*
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler
import org.jetbrains.jet.lang.resolve.scopes.WritableScope
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import java.util.ArrayList
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassStaticsPackageFragmentDescriptor
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor
import org.jetbrains.jet.lang.resolve.name.SpecialNames
import org.jetbrains.jet.lang.types.AbstractClassTypeConstructor
import org.jetbrains.jet.lang.resolve.java.lazy.resolveTopLevelClassInModule

class LazyJavaClassDescriptor(
        private val outerC: LazyJavaResolverContextWithTypes,
        containingDeclaration: DeclarationDescriptor,
        internal val fqName: FqName,
        private val jClass: JavaClass
) : ClassDescriptorBase(outerC.storageManager, containingDeclaration, fqName.shortName()), LazyJavaDescriptor, JavaClassDescriptor {

    private val c: LazyJavaResolverContextWithTypes = outerC.child(this, jClass.getTypeParameters().toSet());

    {
        c.javaResolverCache.recordClass(jClass, this)
    }

    private val _kind = when {
        jClass.isAnnotationType() -> ClassKind.ANNOTATION_CLASS
        jClass.isInterface() -> ClassKind.TRAIT
        jClass.isEnum() -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

    private val _modality = if (jClass.isAnnotationType())
                                Modality.FINAL
                            else Modality.convertFromFlags(jClass.isAbstract() || jClass.isInterface(), !jClass.isFinal())

    private val _visibility = jClass.getVisibility()
    private val _isInner = jClass.getOuterClass() != null && !jClass.isStatic()

    override fun getKind() = _kind
    override fun getModality() = _modality
    override fun getVisibility() = _visibility
    override fun isInner() = _isInner

    private val _typeConstructor = c.storageManager.createLazyValue { LazyJavaClassTypeConstructor() }
    override fun getTypeConstructor(): TypeConstructor = _typeConstructor()

    private val _scopeForMemberLookup = LazyJavaClassMemberScope(c, this, jClass)
    override fun getScopeForMemberLookup() = _scopeForMemberLookup

    private val _innerClassesScope = InnerClassesScopeWrapper(getScopeForMemberLookup())
    override fun getUnsubstitutedInnerClassesScope(): JetScope = _innerClassesScope

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    private val _classObjectDescriptor = c.storageManager.createNullableLazyValue {
        if (jClass.isEnum()) {
            val classObject = MutableClassDescriptor(this, this.getUnsubstitutedInnerClassesScope(), ClassKind.CLASS_OBJECT, false,
                                                     SpecialNames.getClassObjectName(this.getName()))
            classObject.setSupertypes(listOf(KotlinBuiltIns.getInstance().getAnyType()))
            classObject.setModality(Modality.FINAL)
            classObject.setVisibility(jClass.getVisibility())
            classObject.setTypeParameterDescriptors(listOf())
            classObject.createTypeConstructor()
            classObject.setPrimaryConstructor(DescriptorFactory.createPrimaryConstructorForObject(classObject))

            val scope = LazyJavaClassMemberScope(c, classObject, jClass, enumClassObject = true)
            val writableScope = WritableScopeImpl(scope, classObject, RedeclarationHandler.THROW_EXCEPTION, "Enum class object scope")
            writableScope.changeLockLevel(WritableScope.LockLevel.BOTH)

            classObject.setScopeForMemberLookup(writableScope)

            createEnumSyntheticMethods(classObject, this.getDefaultType())

            classObject
        }
        else null
    }

    private fun createEnumSyntheticMethods(classObject: MutableClassDescriptor, enumType: JetType) {
        val valuesReturnType = KotlinBuiltIns.getInstance().getArrayType(enumType)
        val valuesMethod = DescriptorFactory.createEnumClassObjectValuesMethod(classObject, valuesReturnType)
        classObject.getBuilder().addFunctionDescriptor(valuesMethod)

        val valueOfMethod = DescriptorFactory.createEnumClassObjectValueOfMethod(classObject, enumType)
        classObject.getBuilder().addFunctionDescriptor(valueOfMethod)
    }

    override fun getCorrespondingPackageFragment() =
            c.packageFragmentProvider.getPackageFragment(fqName) as? JavaClassStaticsPackageFragmentDescriptor

    override fun getClassObjectDescriptor(): ClassDescriptor? = _classObjectDescriptor()
    override fun getClassObjectType(): JetType? = getClassObjectDescriptor()?.let { d -> d.getDefaultType() }

    override fun getConstructors() = _scopeForMemberLookup._constructors()

    private val _annotations = c.storageManager.createLazyValue { c.resolveAnnotations(jClass) }
    override fun getAnnotations() = _annotations()

    private val _functionTypeForSamInterface = c.storageManager.createNullableLazyValue {
        val samInterfaceMethod = SingleAbstractMethodUtils.getSamInterfaceMethod(jClass);
        if (samInterfaceMethod != null) {
            val abstractMethod = resolveFunctionOfSamInterface(samInterfaceMethod);
            SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod);
        }
        else null
    }

    override fun getFunctionTypeForSamInterface(): JetType? = _functionTypeForSamInterface()

    private fun resolveFunctionOfSamInterface(samInterfaceMethod: JavaMethod): SimpleFunctionDescriptor {
        val methodContainer = samInterfaceMethod.getContainingClass()
        val containerFqName = methodContainer.getFqName()
        assert(containerFqName != null, "qualified name is null for " + methodContainer)
        if (fqName == containerFqName) {
            return _scopeForMemberLookup.resolveMethodToFunctionDescriptor(samInterfaceMethod, false)
        }
        else {
            return findFunctionWithMostSpecificReturnType(TypeUtils.getAllSupertypes(getDefaultType()))
        }
    }

    private fun findFunctionWithMostSpecificReturnType(supertypes: Set<JetType>): SimpleFunctionDescriptor {
        val candidates = ArrayList<SimpleFunctionDescriptor>(supertypes.size())
        for (supertype in supertypes) {
            val abstractMembers = SingleAbstractMethodUtils.getAbstractMembers(supertype)
            if (!abstractMembers.isEmpty()) {
                candidates.add((abstractMembers[0] as SimpleFunctionDescriptor))
            }
        }
        if (candidates.isEmpty()) {
            throw IllegalStateException("Couldn't find abstract method in supertypes " + supertypes)
        }
        var currentMostSpecificType = candidates[0]
        for (candidate in candidates) {
            val candidateReturnType = candidate.getReturnType()
            val currentMostSpecificReturnType = currentMostSpecificType.getReturnType()
            assert(candidateReturnType != null && currentMostSpecificReturnType != null, "$candidate, $currentMostSpecificReturnType")
            if (JetTypeChecker.DEFAULT.isSubtypeOf(candidateReturnType!!, currentMostSpecificReturnType!!)) {
                currentMostSpecificType = candidate
            }
        }
        return currentMostSpecificType
    }

    override fun toString() = "lazy java class $fqName"

    private inner class LazyJavaClassTypeConstructor : AbstractClassTypeConstructor() {

        private val _parameters = c.storageManager.createLazyValue {
            jClass.getTypeParameters().map({
                p ->
                c.typeParameterResolver.resolveTypeParameter(p)
                    ?: throw AssertionError("Parameter $p surely belongs to class ${jClass}, so it must be resolved")
            })
        }

        override fun getParameters(): List<TypeParameterDescriptor> = _parameters()

        private val _supertypes = c.storageManager.createLazyValue<Collection<JetType>> {
            val supertypes = jClass.getSupertypes()
            if (supertypes.isEmpty()) {
                val objectFqName = DescriptorResolverUtils.OBJECT_FQ_NAME
                if (jClass.getFqName() == objectFqName) {
                    listOf(KotlinBuiltIns.getInstance().getAnyType())
                }
                else {
                    val objectType = c.resolveTopLevelClassInModule(objectFqName)?.getDefaultType()
                    // If java.lang.Object is not found, we simply use Any to recover
                    listOf(objectType ?: KotlinBuiltIns.getInstance().getAnyType())
                }
            }
            else
                supertypes.stream()
                        .map {
                            supertype ->
                            c.typeResolver.transformJavaType(supertype, TypeUsage.SUPERTYPE.toAttributes())
                        }
                        .filter { supertype -> !supertype.isError() }
                        .toList()
                        .ifEmpty {
                            listOf(KotlinBuiltIns.getInstance().getAnyType())
                        }
        }

        override fun getSupertypes(): Collection<JetType> = _supertypes()

        override fun getAnnotations() = Annotations.EMPTY

        override fun isFinal() = !getModality().isOverridable()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@LazyJavaClassDescriptor

        override fun toString(): String = getName().asString()
    }
}