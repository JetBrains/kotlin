/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

/**
 * This annotation marks that the annotated method of [Fir2IrDeclarationStorage] or [Fir2IrClassifierStorage] is not very safe and should
 * be used only if you really know what you are doing.
 */
@RequiresOptIn
annotation class DelicateDeclarationStorageApi
