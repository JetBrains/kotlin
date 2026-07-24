// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-js:2.3,2.4
// ISSUE: KT-83356 K/Wasm: Difference in behavior on nested class initialization (for enums?)

// MODULE: lib
// FILE: lib.kt

package lib

var log = ""

enum class FromLibNested(a: String) {
    X("x"),
    Y("y");

    init {
        log += "FromLibNested.init($a);"
    }

    object Nested {
        init {
            log += "FromLibNested.Nested.init;"
        }

        fun ping() {
            log += "FromLibNested.Nested.ping;"
        }

        fun entry(): FromLibNested {
            log += "FromLibNested.Nested.entry.before;"
            val result = X
            log += "FromLibNested.Nested.entry.after;"
            return result
        }
    }

    class RegularNested {
        init {
            log += "FromLibNested.RegularNested.init;"
        }

        fun ping() {
            log += "FromLibNested.RegularNested.ping;"
        }
    }
}

enum class FromLibCompanion(a: String) {
    X("x"),
    Y("y");

    init {
        log += "FromLibCompanion.init($a);"
    }

    companion object Named {
        init {
            log += "FromLibCompanion.Named.init;"
        }

        fun ping() {
            log += "FromLibCompanion.Named.ping;"
        }
    }
}

fun resetLog() {
    log = ""
}

// MODULE: main(lib)
// FILE: main.kt

import lib.*

fun checkLog(testName: String, expected: String): String? {
    if (log != expected) return "$testName: expected <$expected>, actual <$log>"
    resetLog()
    return null
}

fun box(): String {
    FromLibNested.Nested.ping()
    var failure = checkLog(
        "cross-module nested object access",
        "FromLibNested.Nested.init;FromLibNested.Nested.ping;"
    )
    if (failure != null) return failure

    val regularNested = FromLibNested.RegularNested()
    regularNested.ping()
    failure = checkLog(
        "cross-module regular nested class access",
        "FromLibNested.RegularNested.init;FromLibNested.RegularNested.ping;"
    )
    if (failure != null) return failure

    val nestedEntry = FromLibNested.Nested.entry()
    log += "${nestedEntry.name};"
    failure = checkLog(
        "cross-module nested object explicit enum entry access",
        "FromLibNested.Nested.entry.before;FromLibNested.init(x);FromLibNested.init(y);FromLibNested.Nested.entry.after;X;"
    )
    if (failure != null) return failure

    FromLibCompanion.Named.ping()
    failure = checkLog(
        "cross-module named companion object access",
        "FromLibCompanion.init(x);FromLibCompanion.init(y);FromLibCompanion.Named.init;FromLibCompanion.Named.ping;"
    )
    if (failure != null) return failure

    return "OK"
}
