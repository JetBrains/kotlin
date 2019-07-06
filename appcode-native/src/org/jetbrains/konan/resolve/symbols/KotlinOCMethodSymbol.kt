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

class KotlinOCMethodSymbol(
    stub: ObjCMethod,
    project: Project,
    file: VirtualFile,
    containingClass: OCClassSymbol
) : KotlinOCMemberSymbol(stub, file, containingClass), OCMethodSymbol {

    private lateinit var mySelectors: List<OCMethodSymbol.SelectorPartSymbol>
    private val myReturnType: OCType = stub.returnType.toOCType(project, containingClass)
    private val myIsStatic: Boolean = !stub.isInstanceMethod

    override fun getKind(): OCSymbolKind = OCSymbolKind.METHOD

    override fun getReturnType(receiverType: OCObjectType?, project: Project): OCType {
        return OCMethodSymbolImpl.inferReturnType(this, myReturnType, receiverType, project)
    }

    override fun isOptional(): Boolean = false

    override fun isVararg(): Boolean = false

    override fun getOriginalSymbol(): OCSymbol? = null

    override fun getSelectors(): List<OCMethodSymbol.SelectorPartSymbol> = mySelectors

    @Suppress("UNCHECKED_CAST")
    override fun getParameterSymbols(): List<OCDeclaratorSymbol> = Collections.unmodifiableList(mySelectors) as List<OCDeclaratorSymbol>

    override fun getAssociatedSymbol(project: Project): OCMethodSymbol? = null

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isStatic(): Boolean = myIsStatic

    override fun getNameWithParent(context: OCResolveContext): String = "${if (isStatic) "+" else "-"}[${parent.name} $name]"

    fun setSelectors(selectors: List<OCMethodSymbol.SelectorPartSymbol>) {
        mySelectors = selectors
    }
}