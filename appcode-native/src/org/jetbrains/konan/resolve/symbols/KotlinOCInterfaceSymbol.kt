/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.OCTypeParameterSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCGenericParameterSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCInterfaceSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCInterfaceSymbolImpl
import com.jetbrains.cidr.lang.types.OCReferenceType
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.OCTypeArgument
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.createSuperType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KotlinOCInterfaceSymbol(
    stub: ObjCInterface,
    project: Project,
    file: VirtualFile
) : KotlinOCClassSymbol<KotlinOCInterfaceSymbol.InterfaceState, ObjCInterface>(stub, project, file), OCInterfaceSymbol {

    private val categoryName: String? = stub.categoryName

    override fun getKind(): OCSymbolKind = OCSymbolKind.INTERFACE

    override fun getInterface(project: Project): OCInterfaceSymbol? = this

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isTemplateSymbol(): Boolean = state.isTemplateSymbol

    override fun isVariadicTemplate(): Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun getTemplateParameters(): List<OCTypeParameterSymbol<OCTypeArgument>> = genericParameters as List<OCTypeParameterSymbol<OCTypeArgument>>

    override fun isSpecialization(): Boolean = false

    override fun isExplicitInstantiation(): Boolean = false

    override fun getRequiredTemplateArgumentsCnt(): Int = templateParameters.size

    override fun getTemplateSpecialization(): List<OCTypeArgument>? = null

    override fun getCategoryName(): String? = categoryName

    override fun getSuperType(): OCReferenceType = state.superType

    //todo implement generics
    override fun getGenericParameters(): List<OCGenericParameterSymbol> = emptyList()

    override fun getType(): OCType = OCInterfaceSymbolImpl.getInterfaceTypeImpl(this)

    override fun computeState(stub: ObjCInterface, project: Project): InterfaceState = InterfaceState(this, stub, project)

    class InterfaceState(clazz: KotlinOCInterfaceSymbol, stub: ObjCInterface, project: Project) : ClassState(clazz, stub, project) {
        val superType: OCReferenceType = createSuperType(stub.superClass, stub.superProtocols)
        val isTemplateSymbol: Boolean = stub.generics.isNotEmpty()
    }
}