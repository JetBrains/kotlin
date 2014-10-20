var global: C = C()

abstract class C {
    class object {
        val INSTANCE = C()
    }
}

fun foo(p: C?) {
    val local = C()
    foo(<caret>)
}


// ORDER: p
// ORDER: local
// ORDER: global
// ORDER: C.INSTANCE
// ORDER: object
// ORDER: null
