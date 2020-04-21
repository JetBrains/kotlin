/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.cpp.OCDeclaratorSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol.SelectorPartSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbolImpl
import com.jetbrains.cidr.lang.types.OCObjectType
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.translation.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty

class KtOCMethodSymbol : KtOCMemberSymbol, OCMethodSymbol {
    private lateinit var selectors: List<SelectorPartSymbol>
    private lateinit var returnType: OCType
    private var originalSymbol: KtOCMemberSymbol? = null
    private var isStatic: Boolean = false

    constructor(
        stub: ObjCMethod,
        project: Project,
        file: VirtualFile,
        containingClass: OCClassSymbol,
        selectors: List<SelectorPartSymbol>
    ) : super(stub, stub.name, file, containingClass) {
        this.returnType = stub.returnType.toOCType(project, containingClass)
        this.isStatic = !stub.isInstanceMethod
        this.selectors = selectors
    }

    constructor(
        property: KtOCPropertySymbol,
        stub: ObjCProperty,
        name: String,
        returnType: OCType,
        file: VirtualFile,
        containingClass: OCClassSymbol,
        selectors: List<SelectorPartSymbol>
    ) : super(stub, name, file, containingClass) {
        this.originalSymbol = property
        this.returnType = returnType
        this.isStatic = property.isStatic
        this.selectors = selectors
    }

    constructor() : super()

    override fun getKind(): OCSymbolKind = OCSymbolKind.METHOD

    override fun getReturnType(receiverType: OCObjectType?, project: Project): OCType {
        return OCMethodSymbolImpl.inferReturnType(this, returnType, receiverType, project)
    }

    override fun isOptional(): Boolean = false

    override fun isVararg(): Boolean = false

    override fun isSynthetic(): Boolean = getOriginalSymbol() != null

    override fun getOriginalSymbol(): KtOCMemberSymbol? = originalSymbol

    override fun getSelectors(): List<SelectorPartSymbol> = selectors

    override fun getParameterSymbols(): List<OCDeclaratorSymbol> = selectors.mapNotNull { it.parameter }

    override fun getAssociatedSymbol(project: Project): OCMethodSymbol? = null

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isStatic(): Boolean = isStatic

    override fun getNameWithParent(context: OCResolveContext): String = "${if (isStatic) "+" else "-"}[${parent.name} $name]"
}