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

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.idea.maven.plugins.api.MavenFixedValueReferenceProvider
import org.jetbrains.kotlin.cli.common.arguments.DefaultValues
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion

class MavenLanguageVersionsCompletionProvider : MavenFixedValueReferenceProvider(
    LanguageVersion.values().filter { it.isStable || ApplicationManager.getApplication().isInternal }.map { it.versionString }.toTypedArray()
)

class MavenApiVersionsCompletionProvider : MavenFixedValueReferenceProvider(
    LanguageVersion.values().filter { it.isStable || ApplicationManager.getApplication().isInternal }.map { it.versionString }.toTypedArray()
)

class MavenJvmTargetsCompletionProvider : MavenFixedValueReferenceProvider(
    JvmTarget.values().map(JvmTarget::description).toTypedArray()
)

class MavenJsModuleKindsCompletionProvider : MavenFixedValueReferenceProvider(
    DefaultValues.JsModuleKinds.possibleValues!!.map(StringUtil::unquoteString).toTypedArray()
)

class MavenJsMainCallCompletionProvider : MavenFixedValueReferenceProvider(
    DefaultValues.JsMain.possibleValues!!.map(StringUtil::unquoteString).toTypedArray()
)