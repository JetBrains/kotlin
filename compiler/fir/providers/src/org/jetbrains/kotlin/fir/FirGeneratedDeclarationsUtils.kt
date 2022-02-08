/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension

private object OwnerForGeneratedDeclarationKey : FirDeclarationDataKey()

internal var FirClassLikeDeclaration.ownerGenerator: FirDeclarationGenerationExtension? by FirDeclarationDataRegistry.data(OwnerForGeneratedDeclarationKey)
