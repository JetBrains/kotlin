/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions

object JvmGeneratorExtensions : GeneratorExtensions() {
    override val externalDeclarationOrigin: ((DeclarationDescriptor) -> IrDeclarationOrigin)? = { descriptor ->
        if (descriptor is JavaCallableMemberDescriptor)
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        else
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    }
}
