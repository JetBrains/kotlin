/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import java.nio.file.Path

@KtModuleBuilderDsl
public abstract class KtBinaryModuleBuilder : KtModuleBuilder() {
    private val binaryRoots: MutableList<Path> = mutableListOf()

    /**
     * Adds a [root] to the current library.
     *
     * The [root] can be:
     * * A .jar file for JVM libraries or common metadata KLibs
     * * A directory with a set of .classfiles for JVM Libraries
     * * A Kotlin/Native, Kotlin/Common, Kotlin/JS KLib.
     * In this case, all KLib dependencies should be provided together with the KLib itself.
     */
    public fun addBinaryRoot(root: Path) {
        binaryRoots.add(root)
    }

    /**
     * Adds a collection of [roots] to the current library.
     *
     * See [addBinaryRoot] for details
     *
     * @see addBinaryRoot for details
     */
    public fun addBinaryRoots(roots: Collection<Path>) {
        binaryRoots.addAll(roots)
    }

    protected fun getBinaryRoots(): List<Path> = binaryRoots.distinct()
}