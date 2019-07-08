/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.cpp.OCDeclaratorSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbolImpl
import com.jetbrains.cidr.lang.types.OCObjectType
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import java.util.*

class KotlinOCMethodSymbol : KotlinOCMemberSymbol, OCMethodSymbol {

    private lateinit var selectors: List<OCMethodSymbol.SelectorPartSymbol>
    private lateinit var returnType: OCType
    private var isStatic: Boolean

    constructor(
        stub: ObjCMethod,
        project: Project,
        file: VirtualFile,
        containingClass: OCClassSymbol
    ) : super(stub, file, containingClass) {
        this.returnType = stub.returnType.toOCType(project, containingClass)
        this.isStatic = !stub.isInstanceMethod
    }

    constructor() : super() {
        this.isStatic = false
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.METHOD

    override fun getReturnType(receiverType: OCObjectType?, project: Project): OCType {
        return OCMethodSymbolImpl.inferReturnType(this, returnType, receiverType, project)
    }

    override fun isOptional(): Boolean = false

    override fun isVararg(): Boolean = false

    override fun getOriginalSymbol(): OCSymbol? = null

    override fun getSelectors(): List<OCMethodSymbol.SelectorPartSymbol> = selectors

    @Suppress("UNCHECKED_CAST")
    override fun getParameterSymbols(): List<OCDeclaratorSymbol> = Collections.unmodifiableList(selectors) as List<OCDeclaratorSymbol>

    override fun getAssociatedSymbol(project: Project): OCMethodSymbol? = null

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isStatic(): Boolean = isStatic

    override fun getNameWithParent(context: OCResolveContext): String = "${if (isStatic) "+" else "-"}[${parent.name} $name]"

    fun setSelectors(selectors: List<OCMethodSymbol.SelectorPartSymbol>) {
        this.selectors = selectors
    }
}