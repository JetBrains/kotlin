// TARGET_BACKEND: JS

// MODULE: lib
// FILE: l.kt


fun foo(b: Boolean): String {

    var result = ""

    if (b) {

        class L1<T1>(val t1: T1) {
            fun <F1> foo(f1: F1): String {
                class L2<T2>(tx1: T1, fx1: F1, t2: T2) {

                    init {
                        result += tx1
                        result += fx1
                        result += t2
                    }

                    fun <F2> qux(f2: F2): String {
                        class L3<T3>(val t3: T3) {
                            val d by lazy { "F" + t3 }
                        }
                        return L3("E").d + f2
                    }
                }

                return L2(t1, f1, "C").qux("D")
            }
        }

        val tmp = L1("A").foo("B")
        result += tmp
    } else {
        class L1<T1>(val t1: T1) {
            fun <F1> foo(f1: F1): String {
                class L2<T2>(t1: T1, f1: F1, t2: T2) {

                    init {
                        result += t1
                        result += f1
                        result += t2
                    }

                    fun <F2> qux(f2: F2): String {
                        class L3<T3>(val t3: T3) {
                            val e by lazy { "Z" + t3 }
                        }
                        return L3(5).e + f2
                    }
                }

                return L2(t1, f1, 3).qux(4)
            }
        }

        val tmp = L1(1).foo(2)
        result += tmp
    }


    return result
}

// MODULE: main(lib)
// FILE: m.kt

fun box(): String {
    val r1 = foo(true)
    if (r1 != "ABCFED") return "FAIL1: $r1"

    val r2 = foo(false)
    if (r2 != "123Z54") return "FAIL2: $r2"

    return "OK"
}