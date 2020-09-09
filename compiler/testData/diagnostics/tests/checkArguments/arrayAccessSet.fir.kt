// !DIAGNOSTICS: -UNUSED_PARAMETER

object A {
    operator fun set(x: Int, y: String = "y", z: Double) {
    }
}

object B {
    operator fun set(x: Int, y: String = "y", z: Double = 3.14, w: Char = 'w', v: Boolean) {
    }
}

object D {
    operator fun set(x: Int, vararg y: String, z: Double) {
    }
}

object Z {
    operator fun set() {
    }
}

fun test() {
    A[0] = ""
    A[0] = 2.72

    B[0] = ""
    B[0] = 2.72
    B[0] = true

    <!INAPPLICABLE_CANDIDATE!>D[0] = ""<!>
    <!INAPPLICABLE_CANDIDATE!>D[0] = 2.72<!>

    <!INAPPLICABLE_CANDIDATE!>Z[0] = ""<!>
}
