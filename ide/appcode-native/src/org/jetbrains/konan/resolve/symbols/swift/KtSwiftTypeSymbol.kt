package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveState
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.swift.codeinsight.resolve.processor.SwiftSymbolProcessor
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.*
import com.jetbrains.swift.symbols.impl.SwiftClassSymbolUtil
import org.jetbrains.konan.resolve.translation.StubToSwiftSymbolTranslator
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass

abstract class KtSwiftTypeSymbol<State : KtSwiftTypeSymbol.TypeState, Stub : ObjCClass<*>>
    : KtSwiftLazySymbol<State, Stub>, SwiftTypeSymbol {

    constructor(stub: Stub, project: Project, file: VirtualFile) : super(stub, project, file)

    constructor() : super()

    override fun processMembers(processor: SwiftSymbolProcessor, state: ResolveState): Boolean = processRawMembers(processor, state)

    override fun getInitializers(): List<SwiftInitializerSymbol> = SwiftClassSymbolUtil.getInitializersImpl(state.members)

    override fun processRawMembers(processor: SwiftSymbolProcessor, state: ResolveState): Boolean {
        val members = this.state.members ?: return true
        return SwiftClassSymbolUtil.processMembers<SwiftSymbol>(members, processor, state)
    }

    override fun getContext(): SwiftMemberSymbol? = containingTypeSymbol

    override fun getSwiftType(): SwiftType = SwiftTypeFactory.getInstance().createClassType(this)

    //todo
    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY

    override fun getQualifiedName(): String {
        //todo qualified name!!!
        return name
    }

    override fun getSuperTypes(): List<SwiftClassType> = rawSuperTypes

    override fun getRawMembers(): MostlySingularMultiMap<String, SwiftMemberSymbol> =
        state.members ?: MostlySingularMultiMap.emptyMap()

    override fun getContainingTypeSymbol(): SwiftTypeSymbol? = null

    open class TypeState : StubState {
        var members: MostlySingularMultiMap<String, SwiftMemberSymbol>?

        constructor(clazz: KtSwiftTypeSymbol<*, *>, stub: ObjCClass<*>, project: Project) : super(stub) {
            val translator = StubToSwiftSymbolTranslator(project)
            var map: MostlySingularMultiMap<String, SwiftMemberSymbol>? = null
            for (member in stub.members) {
                val translatedMember = translator.translateMember(member, clazz, clazz.containingFile)
                if (translatedMember != null) {
                    if (map == null) map = MostlySingularMultiMap()
                    map.add(member.name, translatedMember)
                }
            }
            this.members = map
        }

        constructor() : super() {
            this.members = null
        }
    }
}