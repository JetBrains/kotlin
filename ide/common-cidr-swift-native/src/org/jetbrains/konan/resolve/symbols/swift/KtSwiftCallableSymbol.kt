package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.symbols.SwiftCallableSymbol
import com.jetbrains.swift.symbols.SwiftGenericParametersInfo
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

abstract class KtSwiftCallableSymbol : KtSwiftMemberSymbol, SwiftCallableSymbol {
    constructor(
        stub: ObjCMethod,
        file: VirtualFile,
        project: Project,
        containingTypeSymbol: SwiftTypeSymbol
    ) : super(stub, file, project, containingTypeSymbol)

    constructor() : super()

    override lateinit var swiftType: SwiftFunctionType

    // beware: necessary for compiler to generate valid code
    abstract override val swiftDeclaredType: SwiftFunctionType

    override fun isCallable(): Boolean = true

    override fun getThrowKind(): SwiftCallableSymbol.ThrowKind = SwiftCallableSymbol.ThrowKind.NONE

    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY
}