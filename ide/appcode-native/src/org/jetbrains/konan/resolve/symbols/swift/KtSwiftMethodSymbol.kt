package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

class KtSwiftMethodSymbol : KtSwiftMemberSymbol, SwiftFunctionSymbol {
    constructor(stub: ObjCMethod, project: Project, file: VirtualFile, containingTypeSymbol: SwiftTypeSymbol)
        : super(stub, file, project, containingTypeSymbol)

    constructor() : super()

    override fun getDeclarationKind(): SwiftDeclarationKind = SwiftDeclarationKind.method

    override fun getSwiftType(): SwiftFunctionType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStaticness(): SwiftCanBeStatic.Staticness = SwiftCanBeStatic.Staticness.NOT_STATIC

    override fun getThrowKind(): SwiftCallableSymbol.ThrowKind = SwiftCallableSymbol.ThrowKind.NONE

    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY

    fun setParameters(parameters: List<SwiftParameterSymbol>) {
        TODO("not implemented")
    }
}