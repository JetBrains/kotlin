package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KtOCMemberSymbol : KtOCImmediateSymbol, OCMemberSymbol {
    private lateinit var containingClass: OCClassSymbol

    constructor(stub: Stub<*>, name: String, file: VirtualFile, containingClass: OCClassSymbol) : super(stub, name, file) {
        this.containingClass = containingClass
    }

    constructor() : super()

    override fun isGlobal(): Boolean = false

    override fun getParent(): OCClassSymbol = containingClass
}