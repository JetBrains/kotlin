package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtSwiftClassSymbol : KtSwiftTypeSymbol<KtSwiftTypeSymbol.TypeState, ObjCInterface> {
    constructor(stub: ObjCInterface, project: Project, file: VirtualFile) : super(stub, project, file)
    constructor() : super()

    override fun getDeclarationKind(): SwiftDeclarationKind = SwiftDeclarationKind.classDeclaration

    override fun computeState(stub: ObjCInterface, project: Project): TypeState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}