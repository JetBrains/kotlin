/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.XmlSerializer
import org.jetbrains.kotlin.config.LanguageFeature

/**
 * This class is needed here to monkey-patch the same class from the :compiler:cli:cli-common module to make it deserializable
 * with XML parser. The [XmlSerializer] needs the no-arg constructor for this class.
 */
@Suppress("unused")
class ManualLanguageFeatureSetting(
    languageFeature: LanguageFeature?,
    state: LanguageFeature.State?,
    stringRepresentation: String?
) {
    constructor() : this(null, null, null)

    private var _languageFeature: LanguageFeature? = languageFeature
    private var _state: LanguageFeature.State? = state
    private var _stringRepresentation: String? =stringRepresentation

    val languageFeature: LanguageFeature
        get() = _languageFeature!!
    val state: LanguageFeature.State
        get() = _state!!
    val stringRepresentation: String
        get() = _stringRepresentation!!
}
