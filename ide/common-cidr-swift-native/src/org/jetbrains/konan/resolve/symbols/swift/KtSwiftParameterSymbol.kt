/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.*
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.SwiftExpression
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.symbols.*
import com.jetbrains.swift.symbols.impl.variable.TypeAnnotationInfo
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter

class KtSwiftParameterSymbol : KtSwiftImmediateSymbol, SwiftParameterSymbol {
    private lateinit var methodSymbol: SwiftFunctionSymbol
    private lateinit var type: SwiftType

    constructor(
        stub: ObjCParameter,
        project: Project,
        file: VirtualFile,
        methodSymbol: SwiftFunctionSymbol
    ) : super(stub, file, project) {
        this.methodSymbol = methodSymbol
    }

    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.parameter

    override val context: SwiftMemberSymbol?
        get() = methodSymbol

    override val swiftType: SwiftType
        get() = type

    override fun isOptional(): Boolean = false

    override fun isReadOnly(): Boolean = true //todo???

    //todo [medvedev]???
    override fun getModifierAttributes(modifier: SwiftModifierInfo.Modifier): SwiftAttributesInfo = SwiftAttributesInfo.EMPTY

    override fun getNameInfo(): SwiftParameterNameInfo = SwiftParameterNameInfo.create(name, null, true)

    override fun getParent(): SwiftCallableSymbol? = methodSymbol

    //todo [medvedev]???
    override fun isVariadic(): Boolean = false

    override fun getTypeInfo(): SwiftVariableTypeInfo = TypeAnnotationInfo(type)

    override fun getNameWithParent(context: OCResolveContext): String = name

    override val initializer: SwiftExpression?
        get() = null

    //todo [medvedev]???
    override val modifiers: SwiftModifierInfo
        get() = SwiftModifierInfo.EMPTY

    fun setType(type: SwiftType) {
        this.type = type
    }
}
