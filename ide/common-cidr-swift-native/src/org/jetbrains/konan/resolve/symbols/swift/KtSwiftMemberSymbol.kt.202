package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KtSwiftMemberSymbol : KtSwiftImmediateSymbol, SwiftMemberSymbol {
    private lateinit var _containingTypeSymbol: SwiftTypeSymbol

    constructor(
        stub: Stub<*>,
        file: VirtualFile,
        project: Project,
        containingTypeSymbol: SwiftTypeSymbol
    ) : super(stub, file, project) {
        this._containingTypeSymbol = containingTypeSymbol
    }

    constructor() : super()

    override val containingTypeSymbol: SwiftTypeSymbol get() = _containingTypeSymbol

    override val context: SwiftTypeSymbol
        get() = containingTypeSymbol
}
