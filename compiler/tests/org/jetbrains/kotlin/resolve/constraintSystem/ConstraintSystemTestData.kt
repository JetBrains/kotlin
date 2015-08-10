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

package org.jetbrains.kotlin.resolve.constraintSystem

import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.JetType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import java.util.regex.Pattern
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.JetTypeImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations

public class ConstraintSystemTestData(
        context: BindingContext,
        private val project: Project,
        private val typeResolver: TypeResolver
) {
    private val functionFoo: FunctionDescriptor
    private val scopeToResolveTypeParameters: JetScope

    init {
        val functions = context.getSliceContents(BindingContext.FUNCTION)
        functionFoo = findFunctionByName(functions.values(), "foo")
        val function = DescriptorToSourceUtils.descriptorToDeclaration(functionFoo) as JetFunction
        val fooBody = function.getBodyExpression()
        scopeToResolveTypeParameters = context.get(BindingContext.RESOLUTION_SCOPE, fooBody)!!
    }

    private fun findFunctionByName(functions: Collection<FunctionDescriptor>, name: String): FunctionDescriptor {
        return functions.firstOrNull { it.getName().asString() == name } ?:
               throw AssertionError("Function ${name} is not declared")
    }

    fun getParameterDescriptor(name: String): TypeParameterDescriptor {
        return functionFoo.getTypeParameters().firstOrNull { it.getName().asString() == name } ?:
               throw AssertionError("Unsupported type parameter name: $name. You may add it to constraintSystem/declarations.kt")
    }

    fun getType(name: String): JetType {
        val matcher = INTEGER_VALUE_TYPE_PATTERN.matcher(name)
        if (matcher.find()) {
            val number = matcher.group(1)!!
            return JetTypeImpl.create(
                    Annotations.EMPTY, IntegerValueTypeConstructor(number.toLong(), KotlinBuiltIns.getInstance()), false, listOf(),
                    JetScope.Empty
            )
        }
        return typeResolver.resolveType(
            scopeToResolveTypeParameters, JetPsiFactory(project).createType(name),
            JetTestUtils.DUMMY_TRACE, true)
    }
}

private val INTEGER_VALUE_TYPE_PATTERN = Pattern.compile("""IntegerValueType\((\d*)\)""")
