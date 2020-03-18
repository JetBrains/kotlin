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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

class KtSwiftExtensionSymbol : KtSwiftTypeSymbol<KtSwiftExtensionSymbol.ExtensionState, ObjCInterface>, SwiftExtensionSymbol {
    constructor(moduleDescriptor: ModuleDescriptor, stub: ObjCInterface, project: Project, file: VirtualFile)
            : super(moduleDescriptor, stub, project, file)

    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.extensionDeclaration

    override val baseType: SwiftType
        get() = state?.baseType ?: SwiftType.UNKNOWN

    override val docString: String?
        get() = null

    override val rawSuperTypes: List<SwiftClassType>
        get() = state?.superTypes ?: emptyList()

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