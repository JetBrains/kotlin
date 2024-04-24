// FIR_IDENTICAL
// ISSUE: KT-67699
// LANGUAGE: +ContextReceivers

interface Either<out A, out B>

interface Raise<in E>

fun <E, A> either(block: Raise<E>.() -> A): Either<E, A> = null!!

context(Raise<String>)
fun findUser(): String = null!!

context(Raise<String>)
val prop: String get() = null!!

fun main() {
    either {
        findUser()
    }

    either {
        prop
    }
}
