// TARGET_BACKEND: JS

// MODULE: lib
// FILE: l.kt

fun foo(b: Boolean): String {

    var result = ""

    if (b) {

        open class L1() {
            open fun foo(): String {
                return "B"
            }

            fun test1(): String {
                open class L2 : L1() {

                    fun test2(): String {
                        class L3: L2() {
                            override fun foo(): String = "O"
                        }

                        return foo() + L3().foo()
                    }


                }
                return foo() + L2().test2()
            }

        }


        result += L1().test1()

        return result

    } else {
        open class L1() {
            open fun bar(): Int {
                return 42
            }

            fun test1(): String {
                open class L2 : L1() {

                    fun test2(): String {
                        class L3: L2() {
                            override fun bar(): Int = 71
                        }

                        return "" + bar() + "" + L3().bar()
                    }


                }
                return "" + bar() + L2().test2()
            }

        }


        result += L1().test1()
    }


    return result
}

// MODULE: main(lib)
// FILE: main.kt



fun box(): String {
    val r1 = foo(true)

    if (r1 != "BBO") return "FAIL1: $r1"

    val r2 = foo(false)
    if (r2 != "424271") return "FAIL2: $r2"

    return "OK"
}