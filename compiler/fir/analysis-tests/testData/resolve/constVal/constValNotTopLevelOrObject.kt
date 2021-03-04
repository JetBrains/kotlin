const val a = 1

object B {
    const val b = 2
}

class C(<!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val b: Boolean) {
    <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val c = 3
}

class D {
    object E {
        const val e = 4
    }

    companion object K {
        const val k = 4
    }

    val M = object {
        <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val m = 3
    }

    open class O {
        open val y: Int = 8
    }

    val t: O = object : O() {
        <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val x = 15
    }

}

object F {
    class G {
        <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val e = 4
    }
}

fun foo() {
    const val a = "2"
}
