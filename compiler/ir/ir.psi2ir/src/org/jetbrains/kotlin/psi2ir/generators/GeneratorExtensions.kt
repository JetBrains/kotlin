/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

open class GeneratorExtensions {
    open val externalDeclarationOrigin: ((DeclarationDescriptor) -> IrDeclarationOrigin)?
        get() = null
}
