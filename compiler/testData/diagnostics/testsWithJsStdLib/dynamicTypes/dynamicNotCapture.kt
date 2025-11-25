// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: WASM_JS, JVM_IR
// TARGET_PLATFORM: JS

fun baz(s: String) {}

fun bazDynamic(s : dynamic) {}

fun barRegular(f: () -> Unit) {
    f()
}

fun box(): String {
    val x: dynamic = object {
        val el: String = "ok"
    }
    barRegular {
        baz(x.el)
    }

    barRegular {
        bazDynamic(x)
    }
    return "OK"
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, lambdaLiteral, stringLiteral */
