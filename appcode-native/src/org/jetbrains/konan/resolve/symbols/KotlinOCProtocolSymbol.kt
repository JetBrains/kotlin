/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.objc.OCInterfaceSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCProtocolSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCProtocolSymbolImpl
import com.jetbrains.cidr.lang.types.OCReferenceType
import com.jetbrains.cidr.lang.types.OCType
import org.jetbrains.konan.resolve.createSuperType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol

class KotlinOCProtocolSymbol(
    stub: ObjCProtocol,
    project: Project,
    file: VirtualFile
) : KotlinOCClassSymbol<KotlinOCProtocolSymbol.ProtocolState, ObjCProtocol>(stub, project, file), OCProtocolSymbol {

    override fun getKind(): OCSymbolKind = OCSymbolKind.PROTOCOL

    override fun getCategoryName(): String? = null

    override fun getSuperType(): OCReferenceType = state.superType

    override fun getInterface(project: Project): OCInterfaceSymbol? = null

    override fun getType(): OCType = OCProtocolSymbolImpl.getProtocolType(this)

    override fun computeState(stub: ObjCProtocol): ProtocolState = ProtocolState(this, stub)

    class ProtocolState(clazz: KotlinOCProtocolSymbol, stub: ObjCProtocol) : KotlinOCClassSymbol.ClassState(clazz, stub) {
        val superType: OCReferenceType = createSuperType(null, stub.superProtocols)
    }
}