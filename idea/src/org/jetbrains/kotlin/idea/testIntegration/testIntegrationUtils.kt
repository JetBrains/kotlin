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

package org.jetbrains.kotlin.idea.testIntegration

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.extensions.Extensions
import com.intellij.testIntegration.TestFramework
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

fun findSuitableFrameworks(klass: KtClassOrObject): List<TestFramework> {
    val lightClass = klass.toLightClass() ?: return emptyList()
    val frameworks = Extensions.getExtensions(TestFramework.EXTENSION_NAME).filter { it.language == JavaLanguage.INSTANCE }
    return frameworks.firstOrNull { it.isTestClass(lightClass) }?.let { listOf(it) }
           ?: frameworks.filterTo(SmartList<TestFramework>()) { it.isPotentialTestClass(lightClass) }
}
