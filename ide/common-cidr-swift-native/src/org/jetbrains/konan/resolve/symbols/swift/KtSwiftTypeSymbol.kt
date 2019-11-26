package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftGenericParametersInfo
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.konan.resolve.translation.StubToSwiftSymbolTranslator
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass

abstract class KtSwiftTypeSymbol<State : KtSwiftTypeSymbol.TypeState, Stub : ObjCClass<*>>
    : KtSwiftLazySymbol<State, Stub>, SwiftTypeSymbol {

    constructor(stub: Stub, project: Project, file: VirtualFile) : super(stub, project, file)

    constructor() : super()

    override val context: SwiftMemberSymbol?
        get() = containingTypeSymbol

    override val swiftType: SwiftType
        get() = SwiftTypeFactory.getInstance().createClassType(this)

    //todo
    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY

    override val qualifiedName: String
        get() {
            //todo qualified name!!!
            return name
        }

    override val rawMembers: MostlySingularMultiMap<String, SwiftMemberSymbol>
        get() = state.members ?: MostlySingularMultiMap.emptyMap()

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
                    map.add(translatedMember.name, translatedMember)
                }
            }
            this.members = map
        }

        constructor() : super() {
            this.members = null
        }
    }
}