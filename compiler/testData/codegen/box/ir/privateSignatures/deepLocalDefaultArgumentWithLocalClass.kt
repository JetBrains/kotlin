// TARGET_BACKEND: JS

// MODULE: lib
// FILE: l.kt

fun foo(b: Boolean): String {

    var result = ""

    if (b) {

        class L1(private val v: String) {
            open fun foo(
                block: () -> String = {
                    class L2 {
                        fun foo(
                            block2: () -> String = { "A" }
                        ): String = block2()
                    }
                    L2().foo()
                }
            ): String {
                return block() + v
            }
        }

        result += L1("B").foo()

    } else {

        class L1(private val v: Int) {
            open fun foo(
                block: () -> String = {
                    class L2 {
                        fun foo(
                            block2: () -> Int = { 42 }
                        ): String = "" + block2()
                    }
                    L2().foo()
                }
            ): String {
                return block() + v
            }
        }

        result += L1(71).foo()
    }


    return result
}

// MODULE: main(lib)
// FILE: main.kt


fun box(): String {
    val r1 = foo(true)

    if (r1 != "AB") return "FAIL1: $r1"

    val r2 = foo(false)
    if (r2 != "4271") return "FAIL2: $r2"

    return "OK"
}