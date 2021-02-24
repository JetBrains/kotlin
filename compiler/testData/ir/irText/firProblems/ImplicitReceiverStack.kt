// WITH_RUNTIME
// FULL_JDK
// JVM_TARGET: 1.8

interface SymbolOwner<E : SymbolOwner<E>>

interface Symbol<E : SymbolOwner<E>>

interface ReceiverValue {
    val type: String
}

class ImplicitReceiverValue<S : Symbol<*>>(val boundSymbol: S?, override val type: String) : ReceiverValue

abstract class ImplicitReceiverStack : Iterable<ImplicitReceiverValue<*>> {
    abstract operator fun get(name: String?): ImplicitReceiverValue<*>?
}

class PersistentImplicitReceiverStack(
    private val stack: List<ImplicitReceiverValue<*>>
) : ImplicitReceiverStack(), Iterable<ImplicitReceiverValue<*>> {
    override operator fun iterator(): Iterator<ImplicitReceiverValue<*>> {
        return stack.iterator()
    }

    override operator fun get(name: String?): ImplicitReceiverValue<*>? {
        return stack.lastOrNull()
    }
}

fun bar(s: String) {}

fun foo(stack: PersistentImplicitReceiverStack) {
    stack.forEach {
        it.boundSymbol
        bar(it.type)
    }
}

fun box(): String {
    val stack = PersistentImplicitReceiverStack(
        listOf(ImplicitReceiverValue(null, "O"), ImplicitReceiverValue(null, "K"))
    )
    foo(stack)
    return stack.first().type + stack[null]?.type
}
