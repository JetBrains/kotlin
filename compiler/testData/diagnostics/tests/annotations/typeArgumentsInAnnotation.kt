// FIR_IDENTICAL
// ISSUE: KT-48444

annotation class Foo<T>(val s: String)

@Foo<Int>("")
fun foo() {
}