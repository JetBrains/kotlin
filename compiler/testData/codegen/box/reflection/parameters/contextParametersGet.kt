// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT

import kotlin.reflect.KProperty

class TestClass {
    context(s: String) val prop: String get() = "O$s"
}

fun box() = (TestClass::class.members.single { it.name == "prop" } as KProperty<*>)
    .getter.call(TestClass(), "K")
