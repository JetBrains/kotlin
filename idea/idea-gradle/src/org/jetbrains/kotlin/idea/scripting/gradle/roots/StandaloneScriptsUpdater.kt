/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

interface StandaloneScriptsUpdater {
    val standaloneScripts: Collection<String>

    fun addStandaloneScript(path: String)

    fun removeStandaloneScript(path: String): GradleBuildRoot?

    class Changes {
        val new = mutableSetOf<String>()
        val removed = mutableSetOf<String>()
    }

    companion object {
        fun collectChanges(
            delegate: StandaloneScriptsUpdater,
            update: StandaloneScriptsUpdater.() -> Unit
        ): Changes {
            synchronized(delegate) {
                val old = delegate.standaloneScripts.toSet()
                delegate.update()
                val changes = Changes()
                val new = delegate.standaloneScripts

                new.forEach {
                    if (it !in old) changes.new.add(it)
                }

                old.forEach {
                    if (it !in new) changes.removed.add(it)
                }

                return changes
            }
        }
    }
}