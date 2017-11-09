// TRUE

class A {
    fun foo(): Int = 12
}

class B(val a: A) {
    val prop1 get() = a.fo<caret>o()
}

// TODO
// SKIP_ANALYZE_CHECK