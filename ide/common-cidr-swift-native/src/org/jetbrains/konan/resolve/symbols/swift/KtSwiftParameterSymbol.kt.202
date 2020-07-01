package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.SwiftExpression
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.symbols.*
import com.jetbrains.swift.symbols.impl.variable.TypeAnnotationInfo
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter

class KtSwiftParameterSymbol : KtSwiftImmediateSymbol, SwiftParameterSymbol {
    constructor(
        stub: ObjCParameter,
        project: Project,
        file: VirtualFile,
        methodSymbol: SwiftCallableSymbol
    ) : super(stub, file, project) {
        context = methodSymbol
    }

    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.parameter

    override lateinit var context: SwiftCallableSymbol
        private set

    override lateinit var swiftType: SwiftType

    override fun isOptional(): Boolean = false

    override fun isReadOnly(): Boolean = true //todo???

    //todo [medvedev]???
    override fun getModifierAttributes(modifier: SwiftModifierInfo.Modifier): SwiftAttributesInfo = SwiftAttributesInfo.EMPTY

    override fun getNameInfo(): SwiftParameterNameInfo = SwiftParameterNameInfo.create(name, null, true)

    //todo [medvedev]???
    override fun isVariadic(): Boolean = false

    override val typeInfo: SwiftVariableTypeInfo get() = TypeAnnotationInfo(swiftType)

    override val swiftAttributes: SwiftAttributesInfo
        get() = super<KtSwiftImmediateSymbol>.swiftAttributes

    override val initializer: SwiftExpression?
        get() = null

    //todo [medvedev]???
    override val modifiers: SwiftModifierInfo
        get() = SwiftModifierInfo.EMPTY
}
