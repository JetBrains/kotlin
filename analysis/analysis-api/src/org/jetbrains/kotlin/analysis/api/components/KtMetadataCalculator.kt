/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtMetadataCalculator {
    public abstract fun calculate(ktClass: KtClassOrObject): Metadata?
    public abstract fun calculate(ktFile: KtFile): Metadata?
    public abstract fun calculate(ktFiles: Collection<KtFile>): Metadata?

    public object Empty : KtMetadataCalculator() {
        override fun calculate(ktClass: KtClassOrObject): Metadata? = null
        override fun calculate(ktFile: KtFile): Metadata? = null
        override fun calculate(ktFiles: Collection<KtFile>): Metadata? = null
    }
}
