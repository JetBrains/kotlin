/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.idea.maven.plugins.api.MavenFixedValueReferenceProvider
import org.jetbrains.kotlin.cli.common.arguments.DefaultValues
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion

class MavenLanguageVersionsCompletionProvider : MavenFixedValueReferenceProvider(
    LanguageVersion.values().filter { it.isStable || ApplicationManager.getApplication().isInternal }.map { it.versionString }
        .toTypedArray()
)

class MavenApiVersionsCompletionProvider : MavenFixedValueReferenceProvider(
    LanguageVersion.values().filter { it.isStable || ApplicationManager.getApplication().isInternal }.map { it.versionString }
        .toTypedArray()
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