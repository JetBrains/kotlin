// !LANGUAGE: +UnrestrictedBuilderInference
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
    val ret1 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.equals(1)
        // there is `String?.equals` extension
        get().equals("")
    }
    val ret2 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)
        x.equals("")
    }
    val ret3 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)

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

        ""
    }
    val ret4 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x == null) {
            x?.hashCode()
            x?.equals(1)
            x.equals("") // TODO: is it correct?
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
    val ret5 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)

        if (get() == null) {}
        if (get() === null) {}

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

        ""
    }
}