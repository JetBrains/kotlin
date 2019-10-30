package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

class KtSwiftMethodSymbol : KtSwiftMemberSymbol, SwiftFunctionSymbol {
    private lateinit var parameters: List<SwiftParameterSymbol>
    private lateinit var returnType: SwiftType

    constructor(
        stub: ObjCMethod,
        project: Project,
        file: VirtualFile,
        containingTypeSymbol: SwiftTypeSymbol
    ) : super(stub, file, project, containingTypeSymbol)

    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.method

    override val swiftType: SwiftFunctionType
        get() {
            val typeFactory = SwiftTypeFactory.getInstance()
            val domain = typeFactory.createDomainType(parameters)
            val functionType = typeFactory.createFunctionType(domain, returnType, false)
            return typeFactory.createImplicitSelfMethodType(functionType)
        }

    override fun getStaticness(): SwiftCanBeStatic.Staticness = SwiftCanBeStatic.Staticness.NOT_STATIC

    override fun getThrowKind(): SwiftCallableSymbol.ThrowKind = SwiftCallableSymbol.ThrowKind.NONE

    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY

    fun setParameters(parameters: List<SwiftParameterSymbol>) {
        this.parameters = parameters
    }

    fun setReturnType(returnType: SwiftType) {
        this.returnType = returnType
    }
}