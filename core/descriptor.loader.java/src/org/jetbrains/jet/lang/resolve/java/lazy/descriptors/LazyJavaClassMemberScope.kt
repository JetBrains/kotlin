/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl
import java.util.Collections
import org.jetbrains.jet.utils.*
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities

public class LazyJavaClassMemberScope(
        c: LazyJavaResolverContextWithTypes,
        containingDeclaration: ClassDescriptor,
        private val jClass: JavaClass,
        private val enumClassObject: Boolean = false
) : LazyJavaMemberScope(c, containingDeclaration) {

    override fun computeMemberIndex(): MemberIndex {
        return object : ClassMemberIndex(jClass, { !enumClassObject && !it.isStatic() }) {
            // For SAM-constructors
            override fun getAllMethodNames(): Collection<Name> = super.getAllMethodNames() + getAllClassNames()
        }
    }

    internal val _constructors = c.storageManager.createLazyValue {
        jClass.getConstructors().flatMap {
            jCtor ->
            val constructor = resolveConstructor(jCtor, getContainingDeclaration(), jClass.isStatic())
            val samAdapter = resolveSamAdapter(constructor)
            if (samAdapter != null) {
                (samAdapter as ConstructorDescriptorImpl).setReturnType(containingDeclaration.getDefaultType())
                listOf(constructor, samAdapter)
            }
            else
                listOf(constructor)
        } ifEmpty {
            emptyOrSingletonList(createDefaultConstructor())
        }
    }

    private fun resolveSamAdapter(original: ConstructorDescriptor): ConstructorDescriptor? {
        return if (SingleAbstractMethodUtils.isSamAdapterNecessary(original))
                   SingleAbstractMethodUtils.createSamAdapterConstructor(original) as ConstructorDescriptor
               else null
    }

    private fun resolveConstructor(constructor: JavaMethod, classDescriptor: ClassDescriptor, isStaticClass: Boolean): ConstructorDescriptor {
        val constructorDescriptor = ConstructorDescriptorImpl(classDescriptor, Annotations.EMPTY, isPrimary = false)

        val valueParameters = resolveValueParameters(c, constructorDescriptor, constructor.getValueParameters())
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                constructor, false, null, null, valueParameters, Collections.emptyList())

        constructorDescriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                effectiveSignature.getValueParameters(),
                constructor.getVisibility(),
                isStaticClass
        )

        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())

        val signatureErrors = effectiveSignature.getErrors()
        if (!signatureErrors.isEmpty()) {
            c.externalSignatureResolver.reportSignatureErrors(constructorDescriptor, signatureErrors)
        }

        c.javaResolverCache.recordConstructor(constructor, constructorDescriptor)

        return constructorDescriptor
    }

    private fun createDefaultConstructor(): ConstructorDescriptor? {
        val isAnnotation: Boolean = jClass.isAnnotationType()
        if (jClass.isInterface() && !isAnnotation)
            return null

        val classDescriptor = getContainingDeclaration()
        val constructorDescriptor = ConstructorDescriptorImpl(classDescriptor, Annotations.EMPTY, isPrimary = true)
        val typeParameters = classDescriptor.getTypeConstructor().getParameters()
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor)
                              else Collections.emptyList<ValueParameterDescriptor>()

        constructorDescriptor.initialize(typeParameters, valueParameters, getConstructorVisibility(classDescriptor), jClass.isStatic())
        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())
        c.javaResolverCache.recordConstructor(jClass, constructorDescriptor);
        return constructorDescriptor
    }

    private fun getConstructorVisibility(classDescriptor: ClassDescriptor): Visibility {
        val visibility = classDescriptor.getVisibility()
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaVisibilities.PROTECTED_AND_PACKAGE
        }
        return visibility
    }

    private fun createAnnotationConstructorParameters(constructor: ConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        val methods = jClass.getMethods()
        val result = ArrayList<ValueParameterDescriptor>(methods.size())

        for ((index, method) in methods.withIndices()) {
            assert(method.getValueParameters().isEmpty(), "Annotation method can't have parameters: " + method)

            val jReturnType = method.getReturnType() ?: throw AssertionError("Annotation method has no return type: " + method)

            val varargElementType =
                if (index == methods.size() - 1 && jReturnType is JavaArrayType) {
                    c.typeResolver.transformJavaType(
                            jReturnType.getComponentType(),
                            TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes()
                    )
                }
                else null

            val returnType = c.typeResolver.transformJavaType(jReturnType, TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes())

            result.add(ValueParameterDescriptorImpl(
                    constructor,
                    index,
                    Annotations.EMPTY,
                    method.getName(),
                    // Parameters of annotation constructors in Java are never nullable
                    TypeUtils.makeNotNullable(returnType),
                    method.hasAnnotationParameterDefaultValue(),
                    // Nulls are not allowed in annotation arguments in Java
                    varargElementType?.let { TypeUtils.makeNotNullable(it) }
            ))
        }

        return result
    }

    private val nestedClassIndex = c.storageManager.createLazyValue {
        jClass.getInnerClasses().valuesToMap { c -> c.getName() }
    }

    private val enumEntryIndex = c.storageManager.createLazyValue {
        jClass.getFields().filter { it.isEnumEntry() }.valuesToMap { f -> f.getName() }
    }

    private val nestedClasses = c.storageManager.createMemoizedFunctionWithNullableValues {
        (name: Name) ->
        val jNestedClass = nestedClassIndex()[name]
        if (jNestedClass == null) {
            val field = enumEntryIndex()[name]
            if (field != null) {
                EnumEntrySyntheticClassDescriptor.create(c.storageManager, getContainingDeclaration(), name,
                                                         c.storageManager.createLazyValue {
                                                             memberIndex().getAllFieldNames() + memberIndex().getAllMethodNames()
                                                         })
            }
            else null
        }
        else {
            // TODO: this caching is a temporary workaround, should be replaced with properly caching the whole LazyJavaPackageFragmentProvider
            val alreadyResolved = c.javaResolverCache.getClass(jNestedClass)
            if (alreadyResolved != null)
                alreadyResolved
            else LazyJavaClassDescriptor(c,
                                    getContainingDeclaration(),
                                    DescriptorUtils.getFqName(getContainingDeclaration()).child(name).toSafe(),
                                    jNestedClass)
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = if (enumClassObject) null else nestedClasses(name)
    override fun getAllClassNames(): Collection<Name> = nestedClassIndex().keySet() + enumEntryIndex().keySet()

    // TODO
    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()


    override fun getContainingDeclaration(): ClassDescriptor {
        return super.getContainingDeclaration() as ClassDescriptor
    }

    // namespaces should be resolved elsewhere
    override fun getPackage(name: Name) = null

    override fun toString() = "Lazy java member scope for " + jClass.getFqName()
}