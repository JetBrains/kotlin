// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    var result = ""
}

var x = 0

fun builder(c: suspend Controller.() -> Unit): String {
    val cc = Controller()
    c.startCoroutine(cc, EmptyContinuation)
    return cc.result
}

suspend fun foo(i: Int): String {
    x++
    return "$i;"
}

var y = 0

suspend fun bars(p1: String, p2: String, p3: String, p4: String) : String {
    y++
    return p1 + p2 + p3 + p4
}

var z = 0

fun bar(p1: String, p2: String, p3: String, p4: String) : String {
    z++
    return p1 + p2 + p3 + p4
}

fun box(): String {

    var r1 = builder {
        var i = 1
        result = bars(
            bars(foo(i++), foo(i++), foo(i++), foo(i++)),
            bars(foo(i++), foo(i++), foo(i++), foo(i++)),
            bars(foo(i++), foo(i++), foo(i++), foo(i++)),
            bars(foo(i++), foo(i++), foo(i++), foo(i++))
        )
    }

    if (r1 != "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;") return "FAIL1: $r1"

    var r2 = builder {
        var i = 1
        result = bars(
            bar(foo(i++), foo(i++), foo(i++), foo(i++)),
            bar(foo(i++), foo(i++), foo(i++), foo(i++)),
            bar(foo(i++), foo(i++), foo(i++), foo(i++)),
            bar(foo(i++), foo(i++), foo(i++), foo(i++))
        )
    }

    if (r2 != "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;") return "FAIL2: $r2"

    var r3 = builder {
        var i = 1
        result = bar(
            bars(foo(i++), foo(i++), foo(i++), foo(i++)),
            bars(foo(i++), foo(i++), foo(i++), foo(i++)),
            bars(foo(i++), foo(i++), foo(i++), foo(i++)),
            bars(foo(i++), foo(i++), foo(i++), foo(i++))
        )
    }

    if (r3 != "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;") return "FAIL3: $r3"

    var r4 = builder {
        var i = 1
        result = bar(
            bar(foo(i++), foo(i++), foo(i++), foo(i++)),
            bar(foo(i++), foo(i++), foo(i++), foo(i++)),
            bar(foo(i++), foo(i++), foo(i++), foo(i++)),
            bar(foo(i++), foo(i++), foo(i++), foo(i++))
        )
    }

    if (r4 != "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;") return "FAIL4: $r4"

    if (x != 4 * 4 * 4) return "FAIL5: $x"

    if (y != 10) return "FAIL6: $y"

    if (z != 10) return "FAIL7: $z"

    return "OK"
}
