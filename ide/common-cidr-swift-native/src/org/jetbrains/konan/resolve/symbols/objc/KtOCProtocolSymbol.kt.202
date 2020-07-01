package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.nullize
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.objc.OCInterfaceSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCProtocolSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCProtocolSymbolImpl
import com.jetbrains.cidr.lang.types.OCReferenceType
import com.jetbrains.cidr.lang.types.OCType
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.konan.resolve.translation.createSuperType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol

class KtOCProtocolSymbol : KtOCClassSymbol<KtOCProtocolSymbol.ProtocolState, ObjCProtocol>, OCProtocolSymbol {
    constructor(translationState: TranslationState<ObjCProtocol>, file: VirtualFile)
            : super(translationState, file)

    constructor() : super()

    override fun getKind(): OCSymbolKind = OCSymbolKind.PROTOCOL

    override fun getCategoryName(): String? = null

    override fun getSuperType(): OCReferenceType = state?.superType ?: OCReferenceType.fromText("")

    override fun getInterface(project: Project): OCInterfaceSymbol? = null

    override fun getType(): OCType = OCProtocolSymbolImpl.getProtocolType(this)

    override fun computeState(stub: ObjCProtocol, project: Project): ProtocolState = ProtocolState(this, stub, project)

    class ProtocolState : ClassState {
        lateinit var superType: OCReferenceType

        constructor(clazz: KtOCProtocolSymbol, stub: ObjCProtocol, project: Project) : super(clazz, stub, project) {
            this.superType = createSuperType(null, stub.superProtocols.nullize())
        }

        constructor() : super()
    }
}