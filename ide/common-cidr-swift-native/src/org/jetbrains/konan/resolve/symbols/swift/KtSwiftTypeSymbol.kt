package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftGenericParametersInfo
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.konan.resolve.translation.KtSwiftSymbolTranslator
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass

abstract class KtSwiftTypeSymbol<State : KtSwiftTypeSymbol.TypeState, Stb : ObjCClass<*>>
    : KtSwiftLazySymbol<State, Stb>, SwiftTypeSymbol {
    constructor(translationState: TranslationState<Stb>, swiftName: String, file: VirtualFile) : super(translationState, swiftName, file)

    constructor() : super()

    internal var containingSymbol: SwiftTypeSymbol? = null

    @Transient
    private var containedSymbols: MutableList<SwiftTypeSymbol>? = null // should only be mutated during symbol building

    internal fun addContainedSymbol(symbol: SwiftTypeSymbol) {
        val containedSymbols = containedSymbols ?: mutableListOf<SwiftTypeSymbol>().also { containedSymbols = it }
        containedSymbols.add(symbol)
    }

    override fun clearTranslationState() {
        super.clearTranslationState()
        containedSymbols = null
    }

    override val context: SwiftTypeSymbol?
        get() = containingSymbol

    override val swiftType: SwiftType
        get() = SwiftTypeFactory.getInstance().createClassType(this)

    //todo
    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY

    override val qualifiedName: String
        get() = containingSymbol?.qualifiedName?.let { "$it.$name" } ?: name

    override val rawMembers: MostlySingularMultiMap<String, SwiftMemberSymbol>
        get() = state?.members ?: MostlySingularMultiMap.emptyMap()

    open class TypeState : StubState {
        var members: MostlySingularMultiMap<String, SwiftMemberSymbol>?

        constructor(clazz: KtSwiftTypeSymbol<*, *>, stub: ObjCClass<*>, project: Project) : super(stub) {
            val members = KtSwiftSymbolTranslator.translateMembers(stub, project, clazz)
            this.members = clazz.containedSymbols?.let { containedSymbols ->
                (members ?: MostlySingularMultiMap<String, SwiftMemberSymbol>()).apply {
                    for (containedSymbol in containedSymbols) {
                        add(containedSymbol.name, containedSymbol)
                    }
                }
            } ?: members
        }

        constructor() : super() {
            members = null
        }
    }
}