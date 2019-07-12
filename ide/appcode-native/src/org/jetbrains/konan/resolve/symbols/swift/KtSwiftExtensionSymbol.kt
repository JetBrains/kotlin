package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.symbols.SwiftExtensionSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtSwiftExtensionSymbol : KtSwiftTypeSymbol<KtSwiftTypeSymbol.TypeState, ObjCInterface>, SwiftExtensionSymbol {
    constructor(stub: ObjCInterface, project: Project, file: VirtualFile) : super(stub, project, file)
    constructor() : super()

    override fun getDeclarationKind(): SwiftDeclarationKind = SwiftDeclarationKind.extensionDeclaration

    override val baseType: SwiftType
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.


    override val docString: String?
        get() = null


    override fun computeState(stub: ObjCInterface, project: Project): TypeState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}