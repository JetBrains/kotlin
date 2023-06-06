// FIR_IDENTICAL
// ISSUE: KT-51793
interface Key

interface Builder {
    operator fun Key.invoke()
}

interface A : Builder
interface B : Builder

val A.k: Key get() = TODO()

fun A.main() {
    fun B.bar() {
        k()
    }
}