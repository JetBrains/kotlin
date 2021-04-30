// WITH_RUNTIME
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR

interface A {
    fun foo(): MutableList<String>
}

fun <K> id(x: K): K = x

fun <K, V> build(@BuilderInference builderAction: MutableMap<K, V>.() -> V) {}

fun <K, V> build2(@BuilderInference builderAction: MutableMap<K, V>.() -> K) {}

fun <K, V> build3(@BuilderInference builderAction: MutableMap<K, V>.(K) -> Unit) {}

fun <K, V> build4(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, Int>) {}

fun <K : V, V : CharSequence> build5(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, V>) {}

fun <K : V, V : CharSequence> build6(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<K, String>) {}

fun <K : V, V : CharSequence> build7(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, V>): MutableMap<String, V> {}

@ExperimentalStdlibApi
fun main() {
    buildList {
        add(<!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>3<!>)
        object : A {
            override fun foo(): MutableList<String> = this@buildList
        }
    }
    buildList {
        add(<!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>3<!>)
        val x: String = get(0)
    }
    buildList {
        add(<!TYPE_MISMATCH, TYPE_MISMATCH!>"3"<!>)
        val x: MutableList<Int> = this@buildList
    }
    buildList {
        val y: CharSequence = ""
        add(<!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>y<!>)
        val x: MutableList<String> = this@buildList
    }
    buildList {
        add("")
        val x: MutableList<CharSequence> = this@buildList
    }
    buildList {
        add(<!TYPE_MISMATCH, TYPE_MISMATCH!>""<!>)
        val x: StringBuilder = get(0)
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
}

fun MutableMap<String, Int>.foo() {}

fun take(x: String) {}
