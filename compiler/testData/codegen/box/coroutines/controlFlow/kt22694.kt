// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

enum class Foo(vararg expected: String) {
    A("start", "A", "end"),
    B("start", "BCD", "end"),
    C("start", "BCD", "end"),
    D("start", "BCD", "end"),
    E("start", "E", "end"),
    F("start", "end");

    val expected = expected.toList()
}

fun box(): String {
    for (c in Foo.values()) {
        val actual = getSequence(c).toList()
        if (actual != c.expected) {
            return "FAIL: -- ${c.expected} != $actual"
        }
    }

    return "OK"
}

fun getSequence(a: Foo) =
    buildSequence {
        yield("start")
        when (a) {
            Foo.A -> {
                yield("A")
            }
            Foo.B,
            Foo.C,
            Foo.D-> {
                yield("BCD")
            }
            Foo.E-> {
                yield("E")
            }
        }
        yield("end")
    }