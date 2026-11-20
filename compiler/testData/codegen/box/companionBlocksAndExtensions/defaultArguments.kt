// ISSUE: KT-91595
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// LANGUAGE: +CompanionBlocks

// Test that static method of classes, we generate the ES6 default parameter syntax even in development mode
// (for other cases like top-level functions we only try to generate it in production mode as an optimization).
// EXPECT_GENERATED_JS: expect=defaultArguments.out.js function=C1$foo;C2$foo TARGET_BACKENDS=JS_IR_ES6

// FILE: log.kt
var log = ""

// FILE: main.kt
class C1 {
    companion {
        // Companion block without fields
        fun foo(x: Int = 10) = "x=$x"
    }
}

class C2 {
    companion {
        // Companion block with a non-trivial field
        val state = run {
            log += "C2.<clinit>;"
            100
        }
        fun foo(x: Int = C3.state) = "x=$x"
    }
}

class C3 {
    companion {
        val state = run {
            log += "C3.<clinit>;"
            20
        }
    }
}

fun box(): String {
    if (C1.foo() != "x=10") return "FAIL: C1.foo() returned ${C1.foo()}"
    if (C2.foo() != "x=20") return "FAIL: C2.foo() returned ${C2.foo()}"
    if (log != "C2.<clinit>;C3.<clinit>;") return "FAIL: Unexpected log: \"$log\""
    return "OK"
}
