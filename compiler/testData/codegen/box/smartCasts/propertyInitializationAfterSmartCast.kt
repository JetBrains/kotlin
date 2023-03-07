// ISSUE: KT-57105

class RootBus: MessageBusImpl()

open class MessageBusImpl {
    val parentBus: Any?

    init {
        this as RootBus
        parentBus = "OK"
    }
}

fun box(): String {
    return RootBus().parentBus as String
}
