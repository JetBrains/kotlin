/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.injection

import org.intellij.plugins.intelliLang.inject.config.BaseInjection

internal class InjectionInfo(val languageId: String?, val prefix: String?, val suffix: String?) {
    fun toBaseInjection(injectionSupport: KotlinLanguageInjectionSupport): BaseInjection? {
        if (languageId == null) return null

        val baseInjection = BaseInjection(injectionSupport.id)
        baseInjection.injectedLanguageId = languageId

        if (prefix != null) {
            baseInjection.prefix = prefix
        }

        if (suffix != null) {
            baseInjection.suffix = suffix
        }

        return baseInjection
    }

    companion object {
        fun fromBaseInjection(baseInjection: BaseInjection?): InjectionInfo? {
            if (baseInjection == null) {
                return null
            }

            return InjectionInfo(
                baseInjection.injectedLanguageId,
                baseInjection.prefix,
                baseInjection.suffix
            )
        }
    }
}