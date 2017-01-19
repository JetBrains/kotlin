/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert

abstract class AbstractKotlinTypeAliasByExpansionShortNameIndexTest : KotlinLightCodeInsightFixtureTestCase() {


    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/"
    }

    lateinit var scope: GlobalSearchScope

    override fun setUp() {
        super.setUp()
        scope = GlobalSearchScope.allScope(project)
    }

    override fun getProjectDescriptor() = super.getProjectDescriptorFromTestName()

    fun doTest(file: String) {
        myFixture.configureByFile(file)
        val fileText = myFixture.file.text
        InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "CONTAINS").forEach {
            assertIndexContains(it)
        }
    }

    private val regex = "\\(key=\"(.*?)\"[, ]*value=\"(.*?)\"\\)".toRegex()

    fun assertIndexContains(record: String) {
        val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
        val (_, key, value) = regex.find(record)!!.groupValues
        val result = index.get(key, project, scope)
        if (value !in result.map { it.name }) {
            Assert.fail(buildString {
                appendln("Record $record not found in index")
                appendln("Index contents:")
                index.getAllKeys(project).asSequence().forEach {
                    appendln("KEY: $it")
                    index.get(it, project, scope).forEach {
                        appendln("    ${it.name}")
                    }
                }
            })
        }
    }

}