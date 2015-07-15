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

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.SPECIAL
import org.jetbrains.kotlin.resolve.calls.inference.registerTypeVariables
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.JetLiteFixture
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.tests.di.createContainerForTests
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.Variance
import java.io.File
import java.util.ArrayList

abstract public class AbstractConstraintSystemTest() : JetLiteFixture() {

    private var _typeResolver: TypeResolver? = null
    private val typeResolver: TypeResolver
        get() = _typeResolver!!

    private var _testDeclarations: ConstraintSystemTestData? = null
    private val testDeclarations: ConstraintSystemTestData
        get() = _testDeclarations!!

    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL)
    }

    override fun setUp() {
        super.setUp()

        _typeResolver = createContainerForTests(getProject(), JetTestUtils.createEmptyModule()).typeResolver
        _testDeclarations = analyzeDeclarations()
    }

    override fun tearDown() {
        _typeResolver = null
        _testDeclarations = null
        super<JetLiteFixture>.tearDown()
    }

    override fun getTestDataPath(): String {
        return super.getTestDataPath() + "/constraintSystem/"
    }

    private fun analyzeDeclarations(): ConstraintSystemTestData {
        val fileName = "declarations.kt"

        val psiFile = createPsiFile(null, fileName, loadFile(fileName))!!
        val bindingContext = JvmResolveUtil.analyzeOneFileWithJavaIntegrationAndCheckForErrors(psiFile).bindingContext
        return ConstraintSystemTestData(bindingContext, getProject(), typeResolver)
    }

    public fun doTest(filePath: String) {
        val constraintsFile = File(filePath)
        val constraintsFileText = constraintsFile.readLines()

        val constraintSystem = ConstraintSystemImpl()

        val variables = parseVariables(constraintsFileText)
        val fixVariables = constraintsFileText.contains("FIX_VARIABLES")
        val typeParameterDescriptors = variables.map { testDeclarations.getParameterDescriptor(it) }
        constraintSystem.registerTypeVariables(typeParameterDescriptors, { Variance.INVARIANT })

        val constraints = parseConstraints(constraintsFileText)
        fun JetType.assertNotError(): JetType {
            assert(!ErrorUtils.containsErrorType(this)) { "Type $this is resolved to or contains error type" }
            return this
        }
        for (constraint in constraints) {
            val firstType = testDeclarations.getType(constraint.firstType).assertNotError()
            val secondType = testDeclarations.getType(constraint.secondType).assertNotError()
            val position = SPECIAL.position()
            when (constraint.kind) {
                MyConstraintKind.SUBTYPE -> constraintSystem.addSubtypeConstraint(firstType, secondType, position)
                MyConstraintKind.SUPERTYPE -> constraintSystem.addSupertypeConstraint(firstType, secondType, position)
                MyConstraintKind.EQUAL -> constraintSystem.addConstraint(
                        ConstraintSystemImpl.ConstraintKind.EQUAL, firstType, secondType, position, topLevel = true)
            }
        }
        if (fixVariables) constraintSystem.fixVariables()

        val resultingStatus = Renderers.RENDER_CONSTRAINT_SYSTEM_SHORT.render(constraintSystem)

        val resultingSubstitutor = constraintSystem.getResultingSubstitutor()
        val result = typeParameterDescriptors.map {
            val parameterType = testDeclarations.getType(it.getName().asString())
            val resultType = resultingSubstitutor.substitute(parameterType, Variance.INVARIANT)
            "${it.getName()}=${resultType?.let { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }}"
        }.join("\n", prefix = "result:\n")

        val boundsFile = File(filePath.replace("constraints", "bounds"))
        JetTestUtils.assertEqualsToFile(boundsFile, "${constraintsFileText.join("\n")}\n\n$resultingStatus\n\n$result")
    }

    class MyConstraint(val kind: MyConstraintKind, val firstType: String, val secondType: String)
    enum class MyConstraintKind
    private constructor(val token: String) {
        SUBTYPE("<:"), SUPERTYPE(">:"), EQUAL(":=")
    }

    private fun parseVariables(lines: List<String>): List<String> {
        val first = lines.first()
        val variablesString = "VARIABLES "
        assert (first.startsWith(variablesString)) { "The first line should contain variables: $first"}
        val variables = first.substringAfter(variablesString).split(' ')
        return variables.toList()
    }

    private fun parseConstraints(lines: List<String>): List<MyConstraint> {
        val kindsMap = MyConstraintKind.values().map { it.token to it }.toMap()
        val kinds = kindsMap.keySet()
        val linesWithConstraints = lines.filter { line -> kinds.any { kind -> line.contains(kind) } }
        return linesWithConstraints.map {
            line ->
            val kind = kinds.first { line.contains(it) }
            val firstType = line.substringBefore(kind).trim()
            val secondType = line.substringAfter(kind).trim()
            MyConstraint(kindsMap[kind]!!, firstType, secondType)
        }
    }
}
