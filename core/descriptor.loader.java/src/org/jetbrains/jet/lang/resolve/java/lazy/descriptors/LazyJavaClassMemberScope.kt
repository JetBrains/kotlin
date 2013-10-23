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
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.resolve.name.LabelName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.utils.emptyList
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.resolve.java.structure.JavaField
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaMethodDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.java.lazy.child
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.jet.lang.resolve.java.lazy.resolveAnnotations
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.lazy.hasNotNullAnnotation
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeAttributes
import org.jetbrains.jet.lang.resolve.java.lazy.hasMutableAnnotation
import org.jetbrains.kotlin.util.iif
import org.jetbrains.jet.lang.resolve.java.lazy.hasReadOnlyAnnotation
import org.jetbrains.jet.utils.valuesToMap
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl
import java.util.Collections
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.JavaConstructorResolver
import org.jetbrains.jet.utils.*
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.types.JetType

public class LazyJavaClassMemberScope(
        c: LazyJavaResolverContextWithTypes,
        containingDeclaration: LazyJavaClassDescriptor,
        private val jClass: JavaClass
) : LazyJavaMemberScope(c, containingDeclaration) {

    override fun computeMemberIndex(): MemberIndex = ClassMemberIndex(jClass, mustBeStatic = false)

    internal val _constructors = c.storageManager.createLazyValue {
        jClass.getConstructors().map {
            jCtor -> resolveConstructor(jCtor, getContainingDeclaration(), jClass.isStatic())
        } ifEmpty {
            emptyOrSingletonList(createDefaultConstructor())
        }
    }

    private fun resolveConstructor(constructor: JavaMethod, classDescriptor: ClassDescriptor, isStaticClass: Boolean): ConstructorDescriptor {
        val constructorDescriptor = ConstructorDescriptorImpl(classDescriptor, Collections.emptyList(), isPrimary = false)

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
        val constructorDescriptor = ConstructorDescriptorImpl(classDescriptor, Collections.emptyList(), isPrimary = true)
        val typeParameters = classDescriptor.getTypeConstructor().getParameters()
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor)
                              else Collections.emptyList<ValueParameterDescriptor>()

        constructorDescriptor.initialize(typeParameters, valueParameters, JavaConstructorResolver.getConstructorVisibility(classDescriptor), jClass.isStatic())
        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())
        c.javaResolverCache.recordConstructor(jClass, constructorDescriptor);
        return constructorDescriptor
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
                    Collections.emptyList(),
                    method.getName(),
                    returnType,
                    method.hasAnnotationParameterDefaultValue(),
                    varargElementType
            ))
        }

        return result
    }

    private val nestedClassIndex = c.storageManager.createLazyValue {
        jClass.getInnerClasses().valuesToMap { c -> c.getName() }
    }

    private val nestedClasses = c.storageManager.createMemoizedFunctionWithNullableValues {
        (name: Name) ->
        val jNestedClass = nestedClassIndex()[name]
        if (jNestedClass == null)
            null
        else
            LazyJavaClassDescriptor(c, getContainingDeclaration(), getContainingDeclaration().fqName.child(name), jNestedClass)
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = nestedClasses(name)
    override fun getAllClassNames(): Collection<Name> = nestedClassIndex().keySet()

    override fun addExtraDescriptors(result: MutableCollection<in DeclarationDescriptor>) {
        // TODO
    }

    // TODO
    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()


    override fun getContainingDeclaration(): LazyJavaClassDescriptor {
        return super.getContainingDeclaration() as LazyJavaClassDescriptor
    }

    // namespaces should be resolved elsewhere
    override fun getNamespace(name: Name): NamespaceDescriptor? = null
    override fun getAllPackageNames(): Collection<Name> = listOf()

    override fun toString() = "Lazy java member scope for " + jClass.getFqName()
}