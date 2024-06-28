// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63828

class FirSession(val name: String)

interface Fir2IrComponents {
    val session: FirSession
    val classifierStorage: Fir2IrClassifierStorage
}

class Fir2IrComponentsStorage(
    override val session: FirSession
) : Fir2IrComponents {
    override lateinit var classifierStorage: Fir2IrClassifierStorage
}

class Fir2IrClassifierStorage(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {
    private val name = session.name
}

fun box(): String {
    val session = FirSession("OK")
    val components = Fir2IrComponentsStorage(session)
    val classifierStorage = Fir2IrClassifierStorage(components)
    components.classifierStorage = classifierStorage
    return classifierStorage.session.name
}
