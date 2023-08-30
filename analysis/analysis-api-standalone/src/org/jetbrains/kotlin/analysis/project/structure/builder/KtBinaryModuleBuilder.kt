/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import java.nio.file.Path

@KtModuleBuilderDsl
public abstract class KtBinaryModuleBuilder : KtModuleBuilder() {
    private val binaryRoots: MutableList<Path> = mutableListOf()

    public fun addBinaryRoot(root: Path) {
        binaryRoots.add(root)
    }

    public fun addBinaryRoots(roots: Collection<Path>) {
        binaryRoots.addAll(roots)
    }

    protected fun getBinaryRoots(): List<Path> = binaryRoots.distinct()
}