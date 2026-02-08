// KJS_WITH_FULL_RUNTIME
package foo
import kotlin.test.*

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

var g: Any?
    get() {
        log("g.get")
        return null
    }
    set(v) {
        log("g.set")
    }

public inline fun Array<String>.boo() {
    var a = g
    for (element in this);
}

public inline fun Iterable<String>.boo(i: Any?) {
    var a = i
    for (element in this);
}

fun test1(f: () -> Array<String>) {
    f().boo()
}

fun test2(f: () -> Iterable<String>) {
    f().boo(g)
}


fun box(): String {
    test1 { log("lambda1"); arrayOf() }
    assertEquals("lambda1;g.get;", pullLog())

    test2 { log("lambda2"); listOf() }
    assertEquals("lambda2;g.get;", pullLog())

    return "OK"
}