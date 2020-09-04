package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.symbols.SwiftCanBeStatic.Staticness
import com.jetbrains.swift.symbols.SwiftFunctionSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

class KtSwiftMethodSymbol : KtSwiftCallableSymbol, SwiftFunctionSymbol {
    private lateinit var staticness: Staticness

    constructor(
        stub: ObjCMethod,
        file: VirtualFile,
        project: Project,
        containingTypeSymbol: SwiftTypeSymbol
    ) : super(stub, file, project, containingTypeSymbol) {
        staticness = if (stub.isInstanceMethod) Staticness.NOT_STATIC else Staticness.CLASS
    }

    constructor() : super()

    override val swiftDeclaredType: SwiftFunctionType
        get() = super.swiftType

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.method

    override fun getStaticness(): Staticness = staticness
}