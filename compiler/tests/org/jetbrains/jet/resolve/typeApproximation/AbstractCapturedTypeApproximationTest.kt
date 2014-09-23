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

package org.jetbrains.jet.resolve.typeApproximation

import org.jetbrains.jet.JetLiteFixture
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.ConfigurationKind
import java.io.File
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.resolve.calls.inference.CapturedTypeConstructor
import org.jetbrains.jet.lang.types.JetTypeImpl
import org.jetbrains.jet.lang.resolve.calls.inference.createCapturedType

abstract public class AbstractCapturedTypeApproximationTest() : JetLiteFixture() {
    override fun createEnvironment(): JetCoreEnvironment = createEnvironmentWithMockJdk(ConfigurationKind.ALL)

    public fun doTest(filePath: String) {
        val file = File(filePath)
        val text = JetTestUtils.doLoadFile(file)!!

        val jetFile = JetPsiFactory(getProject()).createFile(text)
        val bindingContext = JvmResolveUtil.analyzeOneFileWithJavaIntegration(jetFile).bindingContext

        val functions = bindingContext.getSliceContents(BindingContext.FUNCTION)
        val functionFoo = functions.values().firstOrNull { it.getName().asString() == "foo" } ?:
                          throw AssertionError("Function 'foo' is not declared")
        val typeParameter = functionFoo.getTypeParameters().first()
        val parameter = functionFoo.getValueParameters().first().getType()

        val result = StringBuilder {
            val endIndex = text.indexOf("// T captures")
            appendln(if (endIndex == -1) text else text.substring(0, endIndex).trimTrailing())

            for (variance in listOf(Variance.IN_VARIANCE, Variance.OUT_VARIANCE)) {

                val captured = createCapturedType(TypeProjectionImpl(variance, KotlinBuiltIns.getInstance().getIntType()))
                val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter.getTypeConstructor() to TypeProjectionImpl(captured)))
                val typeWithCapturedType = typeSubstitutor.substituteWithoutApproximation(
                        TypeProjectionImpl(Variance.INVARIANT, parameter))!!.getType()

                val (lower, upper) = approximateCapturedTypes(typeWithCapturedType)

                appendln()
                appendln("// T captures '${(captured.getConstructor() as CapturedTypeConstructor).typeProjection}'")
                appendln("// lower: $lower")
                appendln("// upper: $upper")
            }
        }.toString()

        JetTestUtils.assertEqualsToFile(file, result)
    }
}
