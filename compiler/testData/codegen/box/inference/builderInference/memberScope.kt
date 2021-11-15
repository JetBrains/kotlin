// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -DEPRECATION -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR

import kotlin.experimental.ExperimentalTypeInference

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit) {}

fun Any.test() {}
fun Any?.test2() {}

fun box(): String {
    val ret = build {
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

        if (x == null) {
            x?.hashCode()
            x?.equals(1)
            // x.equals("") // it'd be an error, because here we try to add constraint `Nothing? < Any`
//            x.hashCode()
//            x.toString()
//            x.test()
            x?.test2()
            x.test2()
        }

        if (x === null) {
            x?.hashCode()
            x?.equals(1)
            // x.equals("") // it'd be an error, because here we try to add constraint `Nothing? < Any`
//            x.hashCode()
//            x.toString()
//            x.test()
            x?.test2()
            x.test2()
        }

        ""
    }

    return "OK"
}
