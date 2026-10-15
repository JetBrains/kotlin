// LANGUAGE: +ContextParameters
context(s: String)
val property: Int
    get() = s.length

class A {
    context(i: Int)
    fun member(p: Long) = i + p
}
