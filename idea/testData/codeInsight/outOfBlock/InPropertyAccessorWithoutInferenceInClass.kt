// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: foao
class A {
    fun foo(): Int = 12
}

class B(val a: A) {
    val prop1: Int
        get() = a.fo<caret>o()
}
