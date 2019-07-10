/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.*
import com.jetbrains.cidr.lang.symbols.cpp.OCDeclaratorSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCSymbolWithQualifiedName
import com.jetbrains.cidr.lang.symbols.expression.OCExpressionSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.OCTypeArgument
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.SwiftExpression
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.symbols.*
import org.jetbrains.konan.resolve.translation.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter

class KtSwiftParameterSymbol : KtSwiftImmediateSymbol, SwiftParameterSymbol {

    constructor(
        stub: ObjCParameter,
        project: Project,
        file: VirtualFile,
        methodSymbol: SwiftFunctionSymbol
    ) : super(stub, file, project) {
    }

    constructor() : super()

    override fun getDeclarationKind(): SwiftDeclarationKind = SwiftDeclarationKind.parameter

    override fun getContext(): SwiftMemberSymbol? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSwiftType(): SwiftType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOptional(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isReadOnly(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getModifierAttributes(modifier: SwiftModifierInfo.Modifier): SwiftAttributesInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNameInfo(): SwiftParameterNameInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParent(): SwiftCallableSymbol? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVariadic(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypeInfo(): SwiftVariableTypeInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNameWithParent(p0: OCResolveContext): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val initializer: SwiftExpression?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val modifiers: SwiftModifierInfo
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.


}
