class A {
    object B {
        object C {

        }
    }
    companion object {
        val B = ""
    }
}

val ab = A.B // property
val abc = A.B.C // object

object D {
    class E {
        object F {

        }
    }
}

val D.E get() = ""

val def = D.E.F // object
// See KT-46409
val de = D.E

enum class G {
    H;

    fun foo() {
        values()
    }

    companion object {
        val H = ""

        fun values(): Int = 42
    }
}

val gh = G.H // companion property
val gv = G.values() // static function
