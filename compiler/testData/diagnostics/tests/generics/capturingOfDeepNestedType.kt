// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class Inv<T>(val x: T)

fun case_1(a: Inv<out Inv<Inv<Inv<Inv<Inv<Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b: Inv<out Inv<out Inv<out Inv<out Inv<Int?>?>?>?>?>? = a.x
    }
}

fun case_2(a: Inv<out Inv<Inv<Inv<Inv<Inv<Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b: Inv<out Inv<out Inv<out Inv<out Inv<Int?>?>?>?>?>? = a.x
        if (b != null) {
            val c = b.x
            if (c != null) {
                val d = c.x
                if (d != null) {
                    val e = d.x
                    if (e != null) {
                        val f = e.x
                        if (f != null) {
                            val g = <!DEBUG_INFO_SMARTCAST!>f<!>.x
                            if (g != null) {
                                takeInt(<!DEBUG_INFO_SMARTCAST!>g<!>)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun takeInt(i: Int) {}