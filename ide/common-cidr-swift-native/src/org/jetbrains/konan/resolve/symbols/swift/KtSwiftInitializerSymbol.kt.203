package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.codeinsight.resolve.unwrapExtension
import com.jetbrains.swift.lang.SwiftNames
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.symbols.SwiftInitializerSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

class KtSwiftInitializerSymbol : KtSwiftCallableSymbol, SwiftInitializerSymbol {
    constructor(
        stub: ObjCMethod,
        file: VirtualFile,
        project: Project,
        containingTypeSymbol: SwiftTypeSymbol
    ) : super(stub, file, project, containingTypeSymbol)

    constructor() : super()

    override fun getName(): String = SwiftNames.INITIALIZER

    override val swiftDeclaredType: SwiftFunctionType
        get() = super.swiftType

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.initializer

    override fun getTargetClass(): SwiftTypeSymbol? = containingTypeSymbol?.unwrapExtension()
}