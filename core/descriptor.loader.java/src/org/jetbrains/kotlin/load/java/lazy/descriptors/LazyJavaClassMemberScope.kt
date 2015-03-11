/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import java.util.Collections
import org.jetbrains.kotlin.utils.*
import java.util.ArrayList
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import java.util.LinkedHashSet

public class LazyJavaClassMemberScope(
        c: LazyJavaResolverContext,
        containingDeclaration: ClassDescriptor,
        private val jClass: JavaClass
) : LazyJavaMemberScope(c, containingDeclaration) {

    override fun computeMemberIndex(): MemberIndex {
        return object : ClassMemberIndex(jClass, { !it.isStatic() }) {
            // For SAM-constructors
            override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name>
                    = super.getMethodNames(nameFilter) + getClassNames(DescriptorKindFilter.CLASSIFIERS, nameFilter)
        }
    }

    internal val constructors = c.storageManager.createLazyValue {
        val constructors = jClass.getConstructors()
        val result = ArrayList<JavaConstructorDescriptor>(constructors.size())
        for (constructor in constructors) {
            val descriptor = resolveConstructor(constructor)
            result.add(descriptor)
            result.addIfNotNull(c.samConversionResolver.resolveSamAdapter(descriptor))
        }
        result ifEmpty { emptyOrSingletonList(createDefaultConstructor()) }
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val functionsFromSupertypes = getFunctionsFromSupertypes(name, getContainingDeclaration())
        result.addAll(DescriptorResolverUtils.resolveOverrides(name, functionsFromSupertypes, result, getContainingDeclaration(), c.errorReporter))
    }

    private fun getFunctionsFromSupertypes(name: Name, descriptor: ClassDescriptor): Set<SimpleFunctionDescriptor> {
          return descriptor.getTypeConstructor().getSupertypes().flatMap {
              it.getMemberScope().getFunctions(name).map { f -> f as SimpleFunctionDescriptor }
          }.toSet()
      }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val propertiesFromSupertypes = getPropertiesFromSupertypes(name, getContainingDeclaration())

        result.addAll(DescriptorResolverUtils.resolveOverrides(name, propertiesFromSupertypes, result, getContainingDeclaration(),
                                                                   c.errorReporter))
    }

    private fun getPropertiesFromSupertypes(name: Name, descriptor: ClassDescriptor): Set<PropertyDescriptor> {
        return descriptor.getTypeConstructor().getSupertypes().flatMap {
            it.getMemberScope().getProperties(name).map { p -> p as PropertyDescriptor }
        }.toSet()
    }

    override fun resolveMethodSignature(
            method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: JetType,
            valueParameters: LazyJavaMemberScope.ResolvedValueParameters
    ): LazyJavaMemberScope.MethodSignatureData {
        val propagated = c.externalSignatureResolver.resolvePropagatedSignature(
                method, getContainingDeclaration(), returnType, null, valueParameters.descriptors, methodTypeParameters)
        val superFunctions = propagated.getSuperMethods()
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                method, !superFunctions.isEmpty(), propagated.getReturnType(),
                propagated.getReceiverType(), propagated.getValueParameters(), propagated.getTypeParameters(),
                propagated.hasStableParameterNames())

        return LazyJavaMemberScope.MethodSignatureData(effectiveSignature, superFunctions, propagated.getErrors() + effectiveSignature.getErrors())
    }

    private fun resolveConstructor(constructor: JavaConstructor): JavaConstructorDescriptor {
        val classDescriptor = getContainingDeclaration()

        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, c.resolveAnnotations(constructor), /* isPrimary = */ false, c.sourceElementFactory.source(constructor)
        )

        val valueParameters = resolveValueParameters(c, constructorDescriptor, constructor.getValueParameters())
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                constructor, false, null, null, valueParameters.descriptors, Collections.emptyList(), false)

        constructorDescriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                effectiveSignature.getValueParameters(),
                constructor.getVisibility()
        )
        constructorDescriptor.setHasStableParameterNames(effectiveSignature.hasStableParameterNames())
        constructorDescriptor.setHasSynthesizedParameterNames(valueParameters.hasSynthesizedNames)

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
        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, Annotations.EMPTY, /* isPrimary = */ true, c.sourceElementFactory.source(jClass)
        )
        val typeParameters = classDescriptor.getTypeConstructor().getParameters()
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor)
                              else Collections.emptyList<ValueParameterDescriptor>()
        constructorDescriptor.setHasSynthesizedParameterNames(false)

        constructorDescriptor.initialize(typeParameters, valueParameters, getConstructorVisibility(classDescriptor))
        constructorDescriptor.setHasStableParameterNames(true)
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

            val attr = TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes(allowFlexible = false)

            val (returnType, varargElementType) =
                    if (index == methods.size() - 1 && jReturnType is JavaArrayType)
                        Pair(c.typeResolver.transformArrayType(jReturnType, attr, isVararg = true),
                             c.typeResolver.transformJavaType(jReturnType.getComponentType(), attr))
                    else
                        Pair(c.typeResolver.transformJavaType(jReturnType, attr), null)

            result.add(ValueParameterDescriptorImpl(
                    constructor,
                    null,
                    index,
                    Annotations.EMPTY,
                    method.getName(),
                    // Parameters of annotation constructors in Java are never nullable
                    TypeUtils.makeNotNullable(returnType),
                    method.hasAnnotationParameterDefaultValue(),
                    // Nulls are not allowed in annotation arguments in Java
                    varargElementType?.let { TypeUtils.makeNotNullable(it) },
                    c.sourceElementFactory.source(method)
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
                                                             memberIndex().getAllFieldNames() + memberIndex().getMethodNames({true})
                                                         }, c.sourceElementFactory.source(field))
            }
            else null
        }
        else {
            LazyJavaClassDescriptor(
                    c, getContainingDeclaration(), DescriptorUtils.getFqName(getContainingDeclaration()).child(name).toSafe(), jNestedClass
            )
        }
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
            DescriptorUtils.getDispatchReceiverParameterIfNeeded(getContainingDeclaration())

    override fun getClassifier(name: Name): ClassifierDescriptor? = nestedClasses(name)

    override fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>
            = nestedClassIndex().keySet() + enumEntryIndex().keySet()

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> =
            memberIndex().getAllFieldNames() +
            getContainingDeclaration().getTypeConstructor().getSupertypes().flatMapTo(LinkedHashSet<Name>()) { supertype ->
                supertype.getMemberScope().getDescriptors(kindFilter, nameFilter).map { variable ->
                    variable.getName()
                }
            }

    // TODO
    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()


    override fun getContainingDeclaration() = super.getContainingDeclaration() as ClassDescriptor

    // namespaces should be resolved elsewhere
    override fun getPackage(name: Name) = null

    override fun toString() = "Lazy java member scope for " + jClass.getFqName()
}
