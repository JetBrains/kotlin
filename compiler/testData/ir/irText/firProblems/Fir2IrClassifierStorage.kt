// FIR_IDENTICAL
class FirSession(val name: String)

interface Fir2IrComponents {
    val session: FirSession
    val classifierStorage: Fir2IrClassifierStorage

    val Int.components: Int
}

class Fir2IrComponentsStorage(
    override val session: FirSession
) : Fir2IrComponents {
    override lateinit var classifierStorage: Fir2IrClassifierStorage

    override val Int.components: Int
        get() = this
}

class Fir2IrClassifierStorage(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {
    private val name = session.name
}
