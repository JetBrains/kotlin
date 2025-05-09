/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator

import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractTypeParameterTypeTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    @OptIn(KaExperimentalApi::class)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val typeSpec = mainModule.testModule.directives.singleOrZeroValue(Directives.TYPE_PARAMETER_TYPE)

            val (typeParameterReferenceText, typeParameterSymbol) = if (typeSpec != null) {
                val (classId, typeParameterName) = parseTypeSpec(typeSpec)
                val classSymbol = findClassJavaAware(classId) ?: error("Class $classId not found")
                val typeParameterSymbol = classSymbol.typeParameters.find { it.name == typeParameterName }
                    ?: error("Type parameter $typeParameterName not found")
                Pair(null, typeParameterSymbol)
            } else {
                val targetExpression = testServices.expressionMarkerProvider
                    .getBottommostElementOfTypeAtCaret(contextFile) as KtTypeParameter
                Pair(targetExpression.text, targetExpression.symbol)
            }

            val ktType = buildTypeParameterType(typeParameterSymbol) {
                isMarkedNullable = false
            }

            buildString {
                if (typeParameterReferenceText != null) {
                    appendLine("${KtTypeParameter::class.simpleName}: $typeParameterReferenceText")
                }
                appendLine("${KaType::class.simpleName}: ${ktType.render(position = Variance.INVARIANT)}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private fun KaSession.findClassJavaAware(classId: ClassId): KaClassSymbol? {
        val javaClass = JavaPsiFacade.getInstance(useSiteModule.project)
            .findClass(classId.asSingleFqName().asString(), analysisScope)

        if (javaClass != null && javaClass !is KtLightClass) {
            val symbol = javaClass.namedClassSymbol
            if (symbol != null) {
                return symbol
            }
        }

        return findClass(classId)
    }

    private fun parseTypeSpec(typeSpec: String): Pair<ClassId, Name> {
        val chunks = typeSpec.split('#', limit = 2)
        assert(chunks.size == 2)

        return ClassId.fromString(chunks[0]) to Name.identifier(chunks[1])
    }

    private object Directives : SimpleDirectivesContainer() {
        val TYPE_PARAMETER_TYPE by stringDirective("Type parameter type to create")
    }
}
