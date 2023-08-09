// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57788

// KT-61141: ImplicitReceiverStack & PersistentImplicitReceiverStack miss fake overrides `forEach` & `spliterator`
// IGNORE_BACKEND: NATIVE

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
