/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.spec

class SpecSentencesStorage {
    lateinit var latestSpecVersion: String
    private val specSentences = mutableMapOf<String, SentencesByLocation>()

    operator fun get(version: String): SentencesByLocation? {
        return specSentences.getOrPut(version) {
            val htmlSpec = HtmlSpecLoader.loadSpec(version) ?: return null
            HtmlSpecSentencesMapBuilder.build(htmlSpec)
        }
    }

    fun getLatest(): SentencesByLocation? {
        return specSentences.getOrPut("latest") {
            val (version, htmlSpec) = HtmlSpecLoader.loadLatestSpec()
            if (htmlSpec == null) return null
            latestSpecVersion = version
            HtmlSpecSentencesMapBuilder.build(htmlSpec)
        }
    }
}