package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftDeclarationSpecifiers
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.SwiftSymbolAttribute
import org.jetbrains.konan.resolve.symbols.KtLazySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KtSwiftLazySymbol<State : KtLazySymbol.StubState, Stb : Stub<*>> : KtLazySymbol<State, Stb>, SwiftSymbol {
    @Transient
    private lateinit var file: VirtualFile
    @Transient
    private lateinit var project: Project

    constructor(stub: Stb, project: Project, file: VirtualFile) : super(stub, project) {
        this.file = file
        this.project = project
    }

    constructor() : super()

    override fun init(project: Project?, file: VirtualFile?) {
        this.file = file!!
        this.project = project!!
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.FOREIGN_ELEMENT
    override fun isGlobal(): Boolean = true

    override fun getProject(): Project = project
    override fun getContainingFile(): VirtualFile = file

    override fun <T : SwiftSymbolAttribute> getSwiftAttribute(attributeType: SwiftAttributesInfo.AttributeType<T>): T? =
        swiftAttributes.getAttribute(attributeType)

    override fun hasSwiftDeclarationSpecifier(specifier: SwiftDeclarationSpecifiers): Boolean =
        swiftAttributes.hasDeclarationSpecifier(specifier)

    override fun hashCodeExcludingOffset(): Int = (name.hashCode() * 31 + file.hashCode()) * 31 + project.hashCode()

    override fun getSwiftAttributes(): SwiftAttributesInfo = SwiftAttributesInfo.EMPTY //todo???

    override fun getObjcName(): String? = name      //todo???
    override fun getShortObjcName(): String? = name          //todo???

}