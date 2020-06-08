/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.codegen.JvmBackendClassResolver
import org.jetbrains.kotlin.codegen.classId
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.org.objectweb.asm.Type

class FirJvmBackendClassResolver(val components: Fir2IrComponents) : JvmBackendClassResolver {
    @DescriptorBasedIr
    override fun resolveToClassDescriptors(type: Type): List<ClassDescriptor> {
        if (type.sort != Type.OBJECT) return emptyList()

        val symbol = components.session.firSymbolProvider.getClassLikeSymbolByFqName(type.classId) ?: return emptyList()
        require(symbol is FirClassSymbol<*>)
        return listOf(components.classifierStorage.getIrClassSymbol(symbol).descriptor)
    }

}