// !LANGUAGE: +UnrestrictedBuilderInference -UseBuilderInferenceOnlyIfNeeded
// WITH_STDLIB
// !DIAGNOSTICS: -OPT_IN_USAGE_ERROR -CAST_NEVER_SUCCEEDS
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, WASM
// FIR status: Initializer type mismatch at first val x = : expected kotlin/collections/MutableList<kotlin/CharSequence>, actual kotlin/collections/MutableList<kotlin/String>

import kotlin.experimental.ExperimentalTypeInference

fun <K> id(x: K): K = x

@OptIn(ExperimentalTypeInference::class)
fun <K, V> build(@BuilderInference builderAction: MutableMap<K, V>.() -> V) {}

@OptIn(ExperimentalTypeInference::class)
fun <K, V> build2(@BuilderInference builderAction: MutableMap<K, V>.() -> K) {}

@OptIn(ExperimentalTypeInference::class)
fun <K, V> build3(@BuilderInference builderAction: MutableMap<K, V>.(K) -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <K, V> build4(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, Int>) {}

@OptIn(ExperimentalTypeInference::class)
fun <K : V, V : CharSequence> build5(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, V>) {}

@OptIn(ExperimentalTypeInference::class)
fun <K : V, V : CharSequence> build6(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<K, String>) {}

@OptIn(ExperimentalTypeInference::class)
fun <K : V, V : CharSequence> build7(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, V>) = mutableMapOf<String, V>()

@ExperimentalStdlibApi
fun box(): String {
    buildList {
        add("")
        val x: MutableList<CharSequence> = this@buildList
    }
    buildMap {
        val x: Function2<String, Char, Char?> = ::put
    }

    build {
        get("")
        ""
    }

    build2 {
        val x: String = this.values.first()
        1
    }

    build2 {
        take(this.values.first())
        1
    }

    build3 { key: String ->
        take(this.values.first())
    }

    build3 { this.foo() }

    build4 { this }

    build4 { this.run { this } }

    build4 { run { this } }
    build4 { id(run { this }) }

    build5 { id(run { this }) }
    build6 { id(run { this }) }

    val x: MutableMap<String, CharSequence> = build7 {
        id(run { this })
    }

    return "OK"
}

fun MutableMap<String, Int>.foo() {}

fun take(x: String) {}
