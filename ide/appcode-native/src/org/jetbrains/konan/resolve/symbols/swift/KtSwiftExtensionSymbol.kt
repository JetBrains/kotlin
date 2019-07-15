package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftExtensionSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtSwiftExtensionSymbol : KtSwiftTypeSymbol<KtSwiftExtensionSymbol.ExtensionState, ObjCInterface>, SwiftExtensionSymbol {
    constructor(stub: ObjCInterface, project: Project, file: VirtualFile) : super(stub, project, file)
    constructor() : super()

    override fun getDeclarationKind(): SwiftDeclarationKind = SwiftDeclarationKind.extensionDeclaration

    override val baseType: SwiftType
        get() = state.baseType

    override val docString: String?
        get() = null

    override fun getRawSuperTypes(): List<SwiftClassType> = state.superTypes

    override fun computeState(stub: ObjCInterface, project: Project): ExtensionState = ExtensionState(this, stub, project)

    class ExtensionState : TypeState {
        lateinit var baseType: SwiftType
        lateinit var superTypes: List<SwiftClassType>

        constructor(ext: KtSwiftExtensionSymbol, stub: ObjCInterface, project: Project) : super(ext, stub, project) {
            this.baseType = SwiftTypeFactory.getInstance().createClassType(stub.name, SwiftContext.of(ext))
            this.superTypes = stub.superProtocols.map { ref -> createClassType(ref, ext) }
        }

        constructor() : super()
    }
}