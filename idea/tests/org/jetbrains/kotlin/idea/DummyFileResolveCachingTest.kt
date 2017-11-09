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

package org.jetbrains.kotlin.idea

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier

class DummyFileResolveCachingTest : LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun test() {
        myFixture.configureByText(KotlinFileType.INSTANCE, "")

        val dummyFileText = "import java.util.Properties"
        val dummyFile = KtPsiFactory(project).createAnalyzableFile("Dummy.kt", dummyFileText, file)

        fun findClassifier(identifier: String) =
                dummyFile.getResolutionFacade()
                        .getFileResolutionScope(dummyFile)
                        .findClassifier(Name.identifier(identifier), NoLookupLocation.FROM_IDE)

        assertNotNull(findClassifier("Properties"))

        dummyFile.importDirectives.single().delete()

        assertNull(findClassifier("Properties"))
    }
}