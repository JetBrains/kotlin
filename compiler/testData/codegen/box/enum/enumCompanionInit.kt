// DONT_TARGET_EXACT_BACKEND: JS
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// DONT_TARGET_EXACT_BACKEND: WASM
var result = ""

enum class E(a: String) {
    X("x"),
    Y("y");

    init {
        result += "E.init($a);"
    }

    companion object {
        init {
            result += "E.companion.init;"
            val value = E.values()[0].name
            result += "$value;"
        }
    }
}

enum class F(a: String) {
    X("x"),
    Y("y");

    init {
        result += "F.init($a);"
    }

    companion object {
        init {
            result += "F.companion.init;"
        }

        fun foo() {
            result += "F.foo();$X;"
        }
    }
}

enum class G(a: String) {
    X("x"),
    Y("y");

    init {
        result += "G.init($a);"
    }

    object O {
        init {
            result += "G.O.init;"
        }

        fun foo() {
            result += "G.O.foo();$X;"
        }
    }
}

fun box(): String {
    val y = E.Y
    result += "${y.name};"
    F.foo()
    G.O.foo()
    if (result != "E.init(x);E.init(y);E.companion.init;X;Y;F.init(x);F.init(y);F.companion.init;F.foo();X;G.O.init;G.O.foo();X;")
        return "fail: $result"

    return "OK"
}
