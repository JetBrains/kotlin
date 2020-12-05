// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: JVM
// ^ KT-43698, fixed in JVM_IR
// WITH_RUNTIME
// JVM_TARGET: 1.8
// FILE: jvmDefaultAll.kt

interface IFooBar {
    val foo get() = "O"
    val bar get() = "Failed"
}

interface IFooBar2 : IFooBar

inline class Test1(override val bar: String): IFooBar

inline class Test2(override val bar: String): IFooBar2

fun box(): String {
    val k = Test1("K")
    val ik: IFooBar = k
    val k2 = Test2("K")
    val ik2: IFooBar = k2
    val ik3: IFooBar2 = k2

    val test1 = k.foo + k.bar
    if (test1 != "OK") throw AssertionError("test1: $test1")

    val test2 = ik.foo + ik.bar
    if (test2 != "OK") throw AssertionError("test2: $test2")

    val test3 = k2.foo + k2.bar
    if (test3 != "OK") throw AssertionError("test3: $test3")

    val test4 = ik2.foo + ik2.bar
    if (test4 != "OK") throw AssertionError("test4: $test4")

    val test5 = ik3.foo + ik3.bar
    if (test5 != "OK") throw AssertionError("test5: $test5")

    return "OK"
}