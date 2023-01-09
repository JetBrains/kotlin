// KT-55828
// IGNORE_BACKEND_K2: NATIVE
private var logs = ""

enum class Foo(val text: String) {
    FOO("foo"),
    BAR("bar"),
    PING("foo");

    init {
        logs += "${text}A;"
    }

    companion object {
        init {
            logs += "StatA;"
        }
        val first = values()[0]
        init {
            logs += "Stat${first.text};"
        }
    }

    init {
        logs += "${text}B;"
    }
}

fun box(): String {
    Foo.FOO

    if (Foo.first !== Foo.FOO) return "FAIL 0: ${Foo.first}"

    if (logs != "fooA;fooB;barA;barB;fooA;fooB;StatA;Statfoo;") return "FAIL 1: ${logs}"

    return "OK"
}