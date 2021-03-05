/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/* Reads serialized IR from annotations in classfiles */
class SingleClassJvmIrProvider(val moduleDescriptor: ModuleDescriptor) : IrProvider {
    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        val toplevelClass = symbol.signature?.let(::findToplevelClassBySignature)
        id(toplevelClass)
        return null
    }

    private fun findToplevelClassBySignature(signature: IdSignature): IrClass? {
        if (signature !is IdSignature.PublicSignature) return null
        val classId = ClassId.topLevel(signature.packageFqName().child(Name.identifier(signature.firstNameSegment)))
        val toplevelDescriptor = moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        val classHeader =
            (toplevelDescriptor.source as? KotlinJvmBinarySourceElement)?.binaryClass?.classHeader ?: return null
        if (classHeader.serializedIr == null || classHeader.serializedIr!!.isEmpty()) return null

        val irProto = JvmIr.JvmIrClass.parseFrom(classHeader.serializedIr)
        /**/id(irProto)
        /**/ return null
    }

    fun id(x: Any?): Any? = x
}