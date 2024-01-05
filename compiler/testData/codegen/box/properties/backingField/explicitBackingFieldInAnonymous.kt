// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

interface I {
    val number: Number
}

fun test1(): String? {
    val it = object : I {
        final override val number: Number
            field = 10

        val next get() = number + 1
    }

    return if (it.next != 11) {
        "[1] ${it.number}, ${it.next}"
    } else {
        null
    }
}

fun test2(): String? {
    class Local : I {
        final override val number: Number
            internal field = 42
    }

    return if (Local().number + 3 != 45) {
        "[2] " + Local().number.toString()
    } else {
        null
    }
}

fun test3(): String? {
    val it = object : I {
        override val number: Number
            field = "100"
            get() {
                return field.length
            }
    }

    return if (it.number != 3) {
        "[3] " + it.number.toString()
    } else {
        null
    }
}

fun box(): String {
    val problem = test1()
        ?: test2()
        ?: test3()

    return if (problem != null) {
        "fail: " + problem
    } else {
        "OK"
    }
}
