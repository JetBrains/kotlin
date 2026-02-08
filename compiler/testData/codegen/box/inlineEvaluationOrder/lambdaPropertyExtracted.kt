// See KT-7674
package foo

inline fun <T> buzz(x: T): T {
    log("buzz($x)")
    return x
}

// CHECK_NOT_CALLED: buzz

private var LOG = ""

fun log(string: String) {
    LOG += "$string;"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

class A(val a: Int) {
    val plus: (Int)->Int
        get() {
            log("get plus fun")
            return {
                log("do plus")
                a + it
            }
        }
}

inline fun <T : Any> id(x: T): T {
    log(x.toString())
    return x
}

fun box(): String {
    assertEquals(3, A(id(1)).plus(id(2)))
    assertEquals("1;get plus fun;2;do plus;", pullLog())
    return "OK"
}