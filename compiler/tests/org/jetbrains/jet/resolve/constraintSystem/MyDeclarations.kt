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

package org.jetbrains.jet.resolve.constraintSystem

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.intellij.psi.PsiElement
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.JetLiteFixture
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.analyzer.AnalyzeExhaust
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.di.InjectorForTests
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory
import org.jetbrains.jet.lang.diagnostics.rendering.Renderers
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import java.io.File
import java.io.IOException
import java.util.Collections
import com.intellij.openapi.project.Project
import java.util.regex.Pattern
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.jet.lang.types.JetTypeImpl
import kotlin.test.assertNotNull

public class MyDeclarations(
        private val context: BindingContext,
        private val project: Project,
        private val typeResolver: TypeResolver
) {
    private val functionFoo: FunctionDescriptor
    private val scopeToResolveTypeParameters: JetScope

    {
        val functions = context.getSliceContents(BindingContext.FUNCTION)
        functionFoo = findFunctionByName(functions.values(), "foo")
        val function = (BindingContextUtils.descriptorToDeclaration(context, functionFoo) as JetFunction)
        val fooBody = function.getBodyExpression()
        scopeToResolveTypeParameters = context.get(BindingContext.RESOLUTION_SCOPE, fooBody)!!
    }

    private fun findFunctionByName(functions: Collection<FunctionDescriptor>, name: String): FunctionDescriptor {
        for (function in functions) {
            if (function.getName().asString() == name) {
                return function
            }
        }
        throw AssertionError("Function ${name} is not declared")
    }

    fun getParameterDescriptor(name: String): TypeParameterDescriptor {
        for (typeParameter in functionFoo.getTypeParameters()) {
            if (typeParameter.getName().asString() == name) {
                return typeParameter;
            }
        }
        throw AssertionError("Unsupported type parameter name: " + name + ".")
    }

    fun getType(name: String): JetType {
        val matcher = INTEGER_VALUE_TYPE_PATTERN.matcher(name)
        if (matcher.find()) {
            val number = matcher.group(1)!!
            return JetTypeImpl(IntegerValueTypeConstructor(number.toLong()), JetScope.EMPTY)
        }
        return typeResolver.resolveType(
            scopeToResolveTypeParameters, JetPsiFactory.createType(project, name),
            JetTestUtils.DUMMY_TRACE, true)
    }
}

private val INTEGER_VALUE_TYPE_PATTERN = Pattern.compile("""IntegerValueType\((\d*)\)""")
