// FIR_IDENTICAL
// SKIP_TXT
abstract class Parent<K>
abstract class DefaultParent<K, X> : Parent<K>()
abstract class TableDerived<K : A> : DefaultParent<K, Int>() {
    fun bar(): K = TODO()
}

interface A {}
interface B : A { fun b() }

fun foo(): Parent<out B> = TODO()

fun main() {
    val w = foo() as? TableDerived ?: return
    w.bar().b()
}
