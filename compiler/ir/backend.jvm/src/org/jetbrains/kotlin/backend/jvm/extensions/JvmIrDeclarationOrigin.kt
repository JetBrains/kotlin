/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

class JvmIrDeclarationOrigin(
    originKind: JvmDeclarationOriginKind,
    element: PsiElement?,
    val declaration: IrDeclaration?,
) : JvmDeclarationOrigin(originKind, element, declaration?.toIrBasedDescriptor(), null)
