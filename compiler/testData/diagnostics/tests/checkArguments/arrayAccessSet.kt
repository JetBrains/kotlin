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
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set() {
    }
}

object W {
    operator fun set(vararg va: Int, value: Int) {
    }
}

fun test() {
    A[0] = <!TYPE_MISMATCH!>""<!>
    A[0] = 2.72

    B[0] = <!TYPE_MISMATCH!>""<!>
    B[0] = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2.72<!>
    B[0] = true

    D[0] = <!TYPE_MISMATCH!>""<!>
    D[0] = 2.72

    Z[<!TOO_MANY_ARGUMENTS!>0<!>] = <!TOO_MANY_ARGUMENTS!>""<!>

    W[0] = 1
}
