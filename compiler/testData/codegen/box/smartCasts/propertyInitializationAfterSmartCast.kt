// ISSUE: KT-57105

class RootBus: MessageBusImpl()

open class MessageBusImpl {
    val parentBus: Any?

    init {
        this as RootBus
        parentBus = "O"
    }
}

class RootBus2: CompositeMessageBus2()

open class CompositeMessageBus2: MessageBusImpl2()

open class MessageBusImpl2 {
    val parentBus: Any?

    init {
        this as RootBus2
        parentBus = "K"
    }
}

fun box(): String {
    return "" + RootBus().parentBus + RootBus2().parentBus
}
