/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.ml

import com.completion.ranker.model.kotlin.MLWhiteBox
import com.intellij.internal.ml.DecisionFunction
import com.intellij.internal.ml.ModelMetadata
import com.intellij.internal.ml.completion.CompletionRankingModelBase
import com.intellij.internal.ml.completion.JarCompletionModelProvider
import com.intellij.lang.Language

@Suppress("UnstableApiUsage")
class KotlinMLRankingProvider : JarCompletionModelProvider("Kotlin", "kotlin_features") {
    override fun createModel(metadata: ModelMetadata): DecisionFunction {
        return object : CompletionRankingModelBase(metadata) {
            override fun predict(features: DoubleArray?): Double = MLWhiteBox.makePredict(features)
        }
    }

    override fun isLanguageSupported(language: Language): Boolean = language.id.equals("kotlin", ignoreCase = true)
}