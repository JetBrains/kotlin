package some

import other.Unresolved as A

class Derived : A {
    val x: A? = null

    fun takeA(a: A) {}
}

// COMPILATION_ERRORS
