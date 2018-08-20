/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.lang.documentation.DocumentationMarkup.*

open class KDocTemplate : Template<StringBuilder> {
    val definition = Placeholder<StringBuilder>()

    val description = Placeholder<StringBuilder>()

    val deprecation = Placeholder<StringBuilder>()

    override fun StringBuilder.apply() {
        append(DEFINITION_START)
        insert(definition)
        append(DEFINITION_END)

        if (!deprecation.isEmpty()) {
            append(SECTIONS_START)
            insert(deprecation)
            append(SECTIONS_END)
        }

        insert(description)
    }

    sealed class DescriptionBodyTemplate : Template<StringBuilder> {
        class Kotlin : DescriptionBodyTemplate() {
            val content = Placeholder<StringBuilder>()
            val sections = Placeholder<StringBuilder>()
            override fun StringBuilder.apply() {
                append(CONTENT_START)
                insert(content)
                append(CONTENT_END)

                append(SECTIONS_START)
                insert(sections)
                append(SECTIONS_END)
            }
        }

        class FromJava : DescriptionBodyTemplate() {
            override fun StringBuilder.apply() {
                append(body)
            }

            lateinit var body: String
        }
    }

    class NoDocTemplate : KDocTemplate() {

        val error = Placeholder<StringBuilder>()

        override fun StringBuilder.apply() {
            insert(error)
        }
    }
}

