// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT

import kotlin.reflect.KProperty

class TestClass {
    context(s: String, i: Int) val Any?.prop: String
        get() = if(i == 2) this.toString() + s else "FAIL"
}

fun box() = (TestClass::class.members.single { it.name == "prop" } as KProperty<*>)
    .getter.call(TestClass(), "K", 2, "O")
