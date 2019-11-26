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

    constructor(stub: Stb, project: Project, file: VirtualFile) : super(stub, project, stub.swiftName) {
        this.file = file
        this.project = project
        this.objcName = stub.name
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

    override fun <T : SwiftSymbolAttribute> getSwiftAttribute(type: SwiftAttributesInfo.AttributeType<T>): T? =
        swiftAttributes.getAttribute(type)

    override fun hasSwiftDeclarationSpecifier(declarationSpecifier: SwiftDeclarationSpecifiers): Boolean =
        swiftAttributes.hasDeclarationSpecifier(declarationSpecifier)

    override fun hashCodeExcludingOffset(): Int = (name.hashCode() * 31 + file.hashCode()) * 31 + project.hashCode()

    override val swiftAttributes: SwiftAttributesInfo
        get() = publicSwiftAttributes //todo???

    override val shortObjcName: String?
        get() = objcName //todo???

    final override lateinit var objcName: String //todo???
        private set
}