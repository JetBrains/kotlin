/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File

/**
 * Holds current mapping from source files to fragments, in the K2 KMP context
 *
 * Note: for KT-62686, we don't really care about removed files: if a source isn't recompiled,
 * it can't get illegal lookups.
 *
 * So there's no need to store previous source-to-fragment mapping
 */
class FragmentContext(
    /**
     * Map from path to source to this source's fragment
     */
    private val fileToFragment: Map<String, String>,
    /**
     * If a fragment isn't refined by any other fragments, it's allowed to have incremental compilation.
     * Otherwise, issues from KT-62686 are applicable.
     */
    private val leafFragments: Set<String>
) {
    /**
     * Returns true, if any file from dirtySet is a part of refined fragment (for example: common, apple, linux).
     * It is relevant to KT-62686, because in k2 "refined" fragments aren't supposed to see symbols from "refining" fragments,
     * but they do.
     *
     * Use of `absolutePath` is coordinated with K2MultiplatformStructure.fragmentSourcesCompilerArgs
     */
    fun dirtySetTouchesNonLeafFragments(dirtySet: Iterable<File>): Boolean {
        return dirtySet.any { file ->
            !leafFragments.contains(fileToFragment[file.absolutePath])
        }
    }

    companion object {
        private fun canCreateFragmentContext(args: CommonCompilerArguments): Boolean {
            // fragmentContext solves k2-only issues; also, in k1 mode we don't even expect the fragment-related args
            val isCorrectLanguageVersion = (LanguageVersion.fromVersionString(args.languageVersion) ?: LanguageVersion.LATEST_STABLE).usesK2

            val hasAllRequiredArguments = listOf(args.fragments, args.fragmentRefines, args.fragmentSources).none {
                it.isNullOrEmpty()
            }
            return isCorrectLanguageVersion && hasAllRequiredArguments
        }

        fun fromCompilerArguments(args: CommonCompilerArguments): FragmentContext? {
            if (!canCreateFragmentContext(args)) {
                return null
            }

            val fileToFragment = args.fragmentSources!!.associate {
                // expected format: -Xfragment-sources=jvmMain:/tmp/<..>/kotlin/main.kt,<...>
                val fragmentSource = it.split(":", limit=2)
                Pair(fragmentSource.last(), fragmentSource.first())
            }
            val refinedFragments = args.fragmentRefines!!.map { fragmentRefine ->
                // expected format: -Xfragment-refines=jvmMain:commonMain,<...>
                fragmentRefine.split(":", limit = 2).last()
            }.toSet()
            val leafFragments = args.fragments!!.toSet() - refinedFragments

            return FragmentContext(fileToFragment, leafFragments)
        }
    }
}