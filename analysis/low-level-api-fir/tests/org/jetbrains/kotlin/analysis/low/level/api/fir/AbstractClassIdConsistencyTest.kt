/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.AbstractStringEnumerator
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractClassIdConsistencyTest.Directives.IGNORE_CONSISTENCY_CHECK
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.utils.ignoreExceptionIfIgnoreDirectivePresent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

abstract class AbstractClassIdConsistencyTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        testServices.moduleStructure.allDirectives.ignoreExceptionIfIgnoreDirectivePresent(IGNORE_CONSISTENCY_CHECK) {
            mainFile.forEachDescendantOfType<KtClassLikeDeclaration> { declaration ->
                val classId = declaration.getClassId()
                val fqName = declaration.safeFqNameForLazyResolve()
                testServices.assertions.assertEquals(fqName, classId?.asSingleFqName())

                testSerialization(classId, testServices)
            }
        }
    }

    private fun testSerialization(classId: ClassId?, testServices: TestServices) {
        val outStream = ByteArrayOutputStream()

        val enumerator = object : AbstractStringEnumerator {
            var list: MutableList<String?> = mutableListOf(null)
            var map: MutableMap<String?, Int> = mutableMapOf(null to 0)

            override fun close() {
            }

            override fun isDirty() = false

            override fun force() {
            }

            override fun enumerate(value: String?): Int {
                return map.getOrElse(value) {
                    map[value] = list.size
                    list += value
                    list.size - 1
                }
            }

            override fun valueOf(idx: Int): String? {
                return list[idx]
            }

            override fun markCorrupted() {
            }
        }

        StubUtils.serializeClassId(StubOutputStream(outStream, enumerator), classId)
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val result = StubUtils.deserializeClassId(StubInputStream(inStream, enumerator))
        testServices.assertions.assertEquals(classId, result)
    }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_CONSISTENCY_CHECK by stringDirective("Temporary disable test until the issue is fixed")
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }
}

abstract class AbstractSourceClassIdConsistencyTest : AbstractClassIdConsistencyTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptClassIdConsistencyTest : AbstractClassIdConsistencyTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
