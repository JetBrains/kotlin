package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.nullize
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftExtensionSymbol
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtSwiftExtensionSymbol : KtSwiftTypeSymbol<KtSwiftExtensionSymbol.ExtensionState, ObjCInterface>, SwiftExtensionSymbol {
    constructor(translationState: TranslationState<ObjCInterface>, swiftName: String, file: VirtualFile)
            : super(translationState, swiftName, file)

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
            this.superTypes = stub.superProtocols.nullize()?.map { ref -> createClassType(ref, ext) } ?: emptyList()
        }

        constructor() : super()
    }
}