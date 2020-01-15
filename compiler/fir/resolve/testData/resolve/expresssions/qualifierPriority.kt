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
val abc = A.B.<!UNRESOLVED_REFERENCE!>C<!> // object

object D {
    class E {
        object F {

        }
    }
}

val D.E get() = ""

val def = D.E.F // object
val de = D.E // extension

enum class G {
    H;
    companion object {
        val H = ""

        fun values(): Int = 42
    }
}

val gh = G.<!AMBIGUITY!>H<!> // companion property
val gv = G.<!AMBIGUITY!>values<!>() // static function
