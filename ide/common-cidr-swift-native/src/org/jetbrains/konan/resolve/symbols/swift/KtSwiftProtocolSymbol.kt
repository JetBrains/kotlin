package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MostlySingularMultiMap
import com.intellij.util.containers.nullize
import com.jetbrains.swift.codeinsight.resolve.SwiftSymbolResult
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.symbols.SwiftAssociatedTypeSymbol
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftProtocolSymbol
import com.jetbrains.swift.symbols.impl.SwiftAllMemberHolder
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol

class KtSwiftProtocolSymbol : KtSwiftTypeSymbol<KtSwiftProtocolSymbol.ProtocolState, ObjCProtocol>, SwiftProtocolSymbol {
    @Transient
    @Volatile
    private var myCachedMemberHolder: SwiftAllMemberHolder? = null

    constructor(translationState: TranslationState<ObjCProtocol>, swiftName: String, file: VirtualFile)
            : super(translationState, swiftName, file)

    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.protocolDeclaration

    override fun getAssociatedTypes(): List<SwiftAssociatedTypeSymbol> = emptyList()

    //todo [medvedev] add AnyObject to super types???
    override fun isClassProtocol(): Boolean = true

    override fun getAllMembers(): MostlySingularMultiMap<String, SwiftSymbolResult<out SwiftMemberSymbol>> {
        if (myCachedMemberHolder == null) {
            myCachedMemberHolder = SwiftAllMemberHolder(this)
        }
        @Suppress("INACCESSIBLE_TYPE")
        return myCachedMemberHolder!!.allMembers
    }

    override val rawSuperTypes: List<SwiftClassType>
        get() = state?.superTypes ?: emptyList()

    override fun computeState(stub: ObjCProtocol, project: Project): ProtocolState = ProtocolState(this, stub, project)

    class ProtocolState : TypeState {
        lateinit var superTypes: List<SwiftClassType>

        constructor(
            protocolSymbol: KtSwiftProtocolSymbol,
            stub: ObjCProtocol,
            project: Project
        ) : super(protocolSymbol, stub, project) {
            this.superTypes = stub.superProtocols.nullize()?.map { ref -> createClassType(ref, protocolSymbol) } ?: emptyList()
        }

        constructor() : super()
    }
}