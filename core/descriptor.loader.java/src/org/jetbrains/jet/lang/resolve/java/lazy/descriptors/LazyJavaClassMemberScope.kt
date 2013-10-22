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

public class LazyJavaClassMemberScope(
        c: LazyJavaResolverContextWithTypes,
        containingDeclaration: LazyJavaClassDescriptor,
        private val jClass: JavaClass,
        classAsPackage: Boolean
) : LazyJavaMemberScope(c, containingDeclaration) {

    private val methodIndex = c.storageManager.createLazyValue {
        jClass.getMethods().iterator().filter { m -> m.isStatic() == classAsPackage && !m.isConstructor() }.groupBy { m -> m.getName() }
    }

    private val _functions = c.storageManager.createMemoizedFunction {
        (name: Name): Collection<FunctionDescriptor>
        ->
        val methods = methodIndex()[name] ?: listOf()
        val functions = methods.map {
            method ->
            val function = JavaMethodDescriptor(
                    containingDeclaration,
                    c.resolveAnnotations(method.getAnnotations()),
                    name
            )
            val innerC = c.child(function, method.getTypeParameters().toSet())
            val valueParameters = resolveValueParameters(innerC, function, method.getValueParameters())
            val returnTypeAttrs = LazyJavaTypeAttributes(c, method, TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
                if (method.hasReadOnlyAnnotation() && !method.hasMutableAnnotation())
                    TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
                else
                    TypeUsage.MEMBER_SIGNATURE_COVARIANT

            }

            function.initialize(
                null,
                DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration),
                method.getTypeParameters().map { p -> innerC.typeParameterResolver.resolveTypeParameter(p) },
                valueParameters,
                c.typeResolver.transformJavaType(method.getReturnType()!!, returnTypeAttrs),
                Modality.convertFromFlags(method.isAbstract(), !method.isFinal()),
                method.getVisibility(),
                false
            )
            function
        }

        // Make sure that lazy things are computed before we release the lock
        for (f in functions) {
            for (p in f.getValueParameters()) {
                p.hasDefaultValue()
            }
        }

        functions
    }

    override fun getFunctions(name: Name) = _functions(name)
    override fun getAllFunctionNames(): Collection<Name> = methodIndex().keySet()

    private fun resolveValueParameters(
            innerC: LazyJavaResolverContextWithTypes,
            function: FunctionDescriptor,
            jValueParameters: List<JavaValueParameter>
    ): List<ValueParameterDescriptor> {
        return jValueParameters.withIndices().map {
            pair ->
            val (index, javaParameter) = pair

            val typeUsage = LazyJavaTypeAttributes(c, javaParameter, TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
                    javaParameter.hasMutableAnnotation().iif(TypeUsage.MEMBER_SIGNATURE_COVARIANT, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT)
            }

            val (outType, varargElementType) =
                if (javaParameter.isVararg()) {
                    val paramType = javaParameter.getType()
                    assert (paramType is JavaArrayType, "Vararg parameter should be an array: $paramType")
                    val arrayType = c.typeResolver.transformArrayType(paramType as JavaArrayType, typeUsage, true)
                    val outType = TypeUtils.makeNotNullable(arrayType)
                    Pair(outType, KotlinBuiltIns.getInstance().getArrayElementType(outType))
                }
                else {
                    val jetType = c.typeResolver.transformJavaType(javaParameter.getType(), typeUsage)
                    if (jetType.isNullable() && javaParameter.hasNotNullAnnotation())
                        Pair(TypeUtils.makeNotNullable(jetType), null)
                    else Pair(jetType, null)
                }

            ValueParameterDescriptorImpl(
                    function,
                    index,
                    innerC.resolveAnnotations(javaParameter.getAnnotations()),
                    // TODO: parameter names may be drawn from attached sources, which is slow; it's better to make them lazy
                    javaParameter.getName() ?: Name.identifier("p$index"),
                    outType,
                    false,
                    varargElementType
            )
        }.toList()
    }


    // TODO
    override fun getProperties(name: Name): Collection<VariableDescriptor> = listOf()
    override fun getAllPropertyNames(): Collection<Name> = listOf()

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
}