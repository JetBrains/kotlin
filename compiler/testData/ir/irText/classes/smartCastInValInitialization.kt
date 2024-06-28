// FIR_IDENTICAL
class RootBus: MessageBusImpl()

open class MessageBusImpl {
    val parentBus: Any?

    init {
        this as RootBus
        parentBus = null
    }
}
