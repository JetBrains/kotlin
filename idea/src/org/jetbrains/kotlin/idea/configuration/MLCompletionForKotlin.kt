/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.completion.settings.CompletionMLRankingSettings
import org.jetbrains.kotlin.idea.KotlinBundle
import kotlin.reflect.jvm.isAccessible

object MLCompletionForKotlinFeature : ExperimentalFeature() {
    override val title: String
        get() = KotlinBundle.message("experimental.ml.completion")

    override fun shouldBeShown(): Boolean = MLCompletionForKotlin.isAvailable

    override var isEnabled: Boolean
        get() = MLCompletionForKotlin.isEnabled
        set(value) {
            MLCompletionForKotlin.isEnabled = value
        }
}

internal object MLCompletionForKotlin {
    const val isAvailable: Boolean = true

    var isEnabled: Boolean
        get() {
            val settings = CompletionMLRankingSettings.getInstance()
            return settings.isRankingEnabledHacked && settings.isLanguageEnabled("Kotlin")
        }
        set(value) {
            val settings = CompletionMLRankingSettings.getInstance()
            if (value && !settings.isRankingEnabledHacked) {
                settings.isRankingEnabledHacked = true
            }

            settings.setLanguageEnabled("Kotlin", value)
        }
}

/**
 * Setter for this property is package-private, so we have to use reflection to change its value.
 */
private var CompletionMLRankingSettings.isRankingEnabledHacked: Boolean
    get() = isRankingEnabled
    set(value) {
        val rankingEnabledSetter = this::class.members.find { it.name == "setRankingEnabled" }
            ?: error("Cannot find 'setRankingEnabled' in class members: ${this::class.members}")

        rankingEnabledSetter.isAccessible = true
        rankingEnabledSetter.call(this, value)
        rankingEnabledSetter.isAccessible = false
    }
