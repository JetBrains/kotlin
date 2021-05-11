// !LANGUAGE: +StableBuilderInference
// !DIAGNOSTICS: -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED
// WITH_RUNTIME

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

@UseExperimental(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

fun Any.test() {}
fun Any?.test2() {}

fun test() {
    val ret = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.equals(1)
        // there is `String?.equals` extension
        get().equals("")
        val x = get()
        x?.hashCode()
        x?.equals(1)
        x.equals("")

        if (get() == null) {}
        if (get() === null) {}

        if (x != null) {
            x?.hashCode()
            x?.equals(1)
            x.equals("")
            x.hashCode()
            x.toString()
            x.test()
            x?.test2()
            x.test2()
        }

        if (x == null) {
            x?.hashCode()
            x?.equals(1)
            x.equals("")
            x.hashCode()
            x.toString()
            x.test()
            x?.test2()
            x.test2()
        }

        if (x === null) {
            x?.hashCode()
            x?.equals(1)
            x.equals("")
            x.hashCode()
            x.toString()
            x.test()
            x?.test2()
            x.test2()
        }

        ""
    }
}