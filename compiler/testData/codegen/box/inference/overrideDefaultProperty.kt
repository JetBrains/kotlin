// MODULE: InterfaceModule
// FILE: MyInterface.kt
interface MyInterface {
    val test: Int get() = 1
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
    override val test = super.test + 1
}

open class MyOpenClassWithOverride2 : MyInterface2, MyOpenClassWithOverride()

open class MyOpenClassWithOverride3 : MyOpenClassWithOverride2() {
    override val test = super.test + 1
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
    if (asInterface(MyFinalClass()).test != 1) return "Fail MyOpenClass as Interface property"
    if (MyFinalClass().test != 1) return "Fail MyOpenClass property"

    if (asInterface(MyFinalClass2()).test != 1) return "Fail MyFinalClass2 as Interface property"
    if (asInterface2(MyFinalClass2()).test != 1) return "Fail MyFinalClass2 as Interface2 property"
    if (MyFinalClass2().test != 1) return "Fail MyFinalClass2 property"

    if (asInterface(MyFinalClass3()).test != 1) return "Fail MyFinalClass3 as Interface property"
    if (MyFinalClass3().test != 1) return "Fail MyFinalClass3 property"

    if (asInterface(MyFinalClassI()).test != 1) return "Fail MyFinalClassI as Interface property"
    if (MyFinalClassI().test != 1) return "Fail MyFinalClassI property"

    if (asInterface(MyFinalClassWithOverride()).test != 2) return "Fail MyFinalClassWithOverride as Interface property"
    if (MyFinalClassWithOverride().test != 2) return "Fail MyFinalClassWithOverride property"

    if (asInterface(MyFinalClassWithOverride2()).test != 2) return "Fail MyFinalClassWithOverride2 as Interface property"
    if (asInterface2(MyFinalClassWithOverride2()).test != 2) return "Fail MyFinalClassWithOverride2 as Interface2 property"
    if (MyFinalClassWithOverride2().test != 2) return "Fail MyFinalClassWithOverride2 property"

    if (asInterface(MyFinalClassWithOverride3()).test != 3) return "Fail MyFinalClassWithOverride3 as Interface property"
    if (asInterface2(MyFinalClassWithOverride3()).test != 3) return "Fail MyFinalClassWithOverride3 as Interface2 property"
    if (MyFinalClassWithOverride3().test != 3) return "Fail MyFinalClassWithOverride3 property"

    if (asInterface(MyFinalClassWithOverrideI()).test != 2) return "Fail MyFinalClassWithOverrideI as Interface property"
    if (MyFinalClassWithOverrideI().test != 2) return "Fail MyFinalClassWithOverrideI property"

    return "OK"
}
