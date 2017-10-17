/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.constraintSystem

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import java.util.regex.Pattern

class ConstraintSystemTestData(
        context: BindingContext,
        private val project: Project,
        private val typeResolver: TypeResolver
) {
    private val functionFoo: FunctionDescriptor
    private val scopeToResolveTypeParameters: LexicalScope

    init {
        val functions = context.getSliceContents(BindingContext.FUNCTION)
        functionFoo = findFunctionByName(functions.values, "foo")
        val function = DescriptorToSourceUtils.descriptorToDeclaration(functionFoo) as KtFunction
        val fooBody = function.bodyExpression
        scopeToResolveTypeParameters = context.get(BindingContext.LEXICAL_SCOPE, fooBody)!!
    }

    private fun findFunctionByName(functions: Collection<FunctionDescriptor>, name: String): FunctionDescriptor {
        return functions.firstOrNull { it.name.asString() == name } ?:
               throw AssertionError("Function ${name} is not declared")
    }

    fun getParameterDescriptor(name: String): TypeParameterDescriptor {
        return functionFoo.typeParameters.firstOrNull { it.name.asString() == name } ?:
               throw AssertionError("Unsupported type parameter name: $name. You may add it to constraintSystem/declarations.kt")
    }

    fun getType(name: String): KotlinType {
        val matcher = INTEGER_VALUE_TYPE_PATTERN.matcher(name)
        if (matcher.find()) {
            val number = matcher.group(1)!!
            return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(Annotations.EMPTY, IntegerValueTypeConstructor(number.toLong(), functionFoo.builtIns),
                                                                         listOf(), false, MemberScope.Empty
            )
        }
        return typeResolver.resolveType(
                scopeToResolveTypeParameters, KtPsiFactory(project).createType(name),
                KotlinTestUtils.DUMMY_TRACE, true)
    }
}

private val INTEGER_VALUE_TYPE_PATTERN = Pattern.compile("""IntegerValueType\((\d*)\)""")
