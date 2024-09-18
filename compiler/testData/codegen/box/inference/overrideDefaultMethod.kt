// TARGET_BACKEND: JS

// Don't test other backends because they don't support overriding and calling interface methods with default arguments

// MODULE: InterfaceModule
// FILE: MyInterface.kt
interface MyInterface {
    fun test() = 1
    fun testWithDefault(x: Int = 1) = x
}

interface MyInterface2 : MyInterface

// MODULE: OpenClassModule(InterfaceModule)
// FILE: MyOpenClass.kt
open class MyOpenClass : MyInterface

open class MyOpenClass2 : MyInterface2

open class MyOpenClass3 : MyOpenClass()

// MODULE: OpenClassWithOverrideModule(InterfaceModule)
// FILE: MyOpenClassWithOverrideModule.kt
open class MyOpenClassWithOverride : MyInterface {
    override fun test() = super.test() + 1
    override fun testWithDefault(x: Int) = super.testWithDefault() + 1
}

open class MyOpenClassWithOverride2 : MyInterface2, MyOpenClassWithOverride()

open class MyOpenClassWithOverride3 : MyOpenClassWithOverride2() {
    override fun test() = super.test() + 1
    override fun testWithDefault(x: Int) = super.testWithDefault(x) + 1
}

// MODULE: main(InterfaceModule, OpenClassModule, OpenClassWithOverrideModule)
// FILE: classes.kt
class MyFinalClass : MyOpenClass()
class MyFinalClass2 : MyOpenClass2()
class MyFinalClass3 : MyOpenClass3()
class MyFinalClassI : MyInterface, MyOpenClass()

class MyFinalClassWithOverride : MyOpenClassWithOverride()
class MyFinalClassWithOverride2 : MyOpenClassWithOverride2()
class MyFinalClassWithOverride3 : MyOpenClassWithOverride3()
class MyFinalClassWithOverrideI : MyInterface, MyOpenClassWithOverride()

// FILE: main.kt
fun asInterface(i: MyInterface): MyInterface = i
fun asInterface2(i: MyInterface2): MyInterface2 = i

fun box(): String {
    if (asInterface(MyFinalClass()).test() != 1) return "Fail MyOpenClass 1"
    if (MyFinalClass().test() != 1) return "Fail MyOpenClass 2"

    if (asInterface(MyFinalClass()).testWithDefault() != 1) return "Fail MyOpenClass 3"
    if (MyFinalClass().testWithDefault() != 1) return "Fail MyOpenClass 4"

    if (asInterface(MyFinalClass2()).test() != 1) return "Fail MyFinalClass2 1"
    if (asInterface2(MyFinalClass2()).test() != 1) return "Fail MyFinalClass2 2"
    if (MyFinalClass2().test() != 1) return "Fail MyFinalClass2 3"

    if (asInterface(MyFinalClass2()).testWithDefault() != 1) return "Fail MyFinalClass2 4"
    if (asInterface2(MyFinalClass2()).testWithDefault() != 1) return "Fail MyFinalClass2 5"
    if (MyFinalClass2().testWithDefault() != 1) return "Fail MyFinalClass2 6"

    if (asInterface(MyFinalClass3()).test() != 1) return "Fail MyFinalClass3 1"
    if (MyFinalClass3().test() != 1) return "Fail MyFinalClass3 2"

    if (asInterface(MyFinalClass3()).testWithDefault() != 1) return "Fail MyFinalClass3 3"
    if (MyFinalClass3().testWithDefault() != 1) return "Fail MyFinalClass3 4"

    if (asInterface(MyFinalClassI()).test() != 1) return "Fail MyFinalClassI 1"
    if (MyFinalClassI().test() != 1) return "Fail MyFinalClassI 2"

    if (asInterface(MyFinalClassI()).testWithDefault() != 1) return "Fail MyFinalClassI 3"
    if (MyFinalClassI().testWithDefault() != 1) return "Fail MyFinalClassI 4"

    if (asInterface(MyFinalClassWithOverride()).test() != 2) return "Fail MyFinalClassWithOverride 1"
    if (MyFinalClassWithOverride().test() != 2) return "Fail MyFinalClassWithOverride 2"

    if (asInterface(MyFinalClassWithOverride()).testWithDefault() != 2) return "Fail MyFinalClassWithOverride 3"
    if (MyFinalClassWithOverride().testWithDefault(1) != 2) return "Fail MyFinalClassWithOverride 4"

    if (asInterface(MyFinalClassWithOverride2()).test() != 2) return "Fail MyFinalClassWithOverride2 1"
    if (asInterface2(MyFinalClassWithOverride2()).test() != 2) return "Fail MyFinalClassWithOverride2 2"
    if (MyFinalClassWithOverride2().test() != 2) return "Fail MyFinalClassWithOverride2 3"

    if (asInterface(MyFinalClassWithOverride2()).testWithDefault() != 2) return "Fail MyFinalClassWithOverride2 4"
    if (asInterface2(MyFinalClassWithOverride2()).testWithDefault() != 2) return "Fail MyFinalClassWithOverride2 5"
    if (MyFinalClassWithOverride2().testWithDefault(1) != 2) return "Fail MyFinalClassWithOverride2 6"

    if (asInterface(MyFinalClassWithOverride3()).test() != 3) return "Fail MyFinalClassWithOverride3 1"
    if (asInterface2(MyFinalClassWithOverride3()).test() != 3) return "Fail MyFinalClassWithOverride3 2"
    if (MyFinalClassWithOverride3().test() != 3) return "Fail MyFinalClassWithOverride3 3"

    if (asInterface(MyFinalClassWithOverride3()).testWithDefault() != 3) return "Fail MyFinalClassWithOverride3 4"
    if (asInterface2(MyFinalClassWithOverride3()).testWithDefault() != 3) return "Fail MyFinalClassWithOverride3 5"
    if (MyFinalClassWithOverride3().testWithDefault(1) != 3) return "Fail MyFinalClassWithOverride3 6"

    if (asInterface(MyFinalClassWithOverrideI()).test() != 2) return "Fail MyFinalClassWithOverrideI 1"
    if (MyFinalClassWithOverrideI().test() != 2) return "Fail MyFinalClassWithOverrideI 2"

    if (asInterface(MyFinalClassWithOverrideI()).testWithDefault() != 2) return "Fail MyFinalClassWithOverrideI 3"
    if (MyFinalClassWithOverrideI().testWithDefault(1) != 2) return "Fail MyFinalClassWithOverrideI 4"

    return "OK"
}
