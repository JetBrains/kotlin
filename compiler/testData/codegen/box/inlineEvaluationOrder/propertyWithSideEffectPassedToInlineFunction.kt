// See KT-7043, KT-11711
package foo

inline fun foo(b: Any) {
    val t = aa[0]
    val a = b
}

private var LOG = ""

fun log(string: String) {
    LOG += "$string;"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

val a: Array<String>
    get() {
        log("a.get")
        return arrayOf("a")
    }

val aa: Array<String>
    get() {
        log("aa.get")
        return arrayOf("aa")
    }

fun box(): String {
    foo(a[0])

    assertEquals("a.get;aa.get;", pullLog())

    return "OK"
}