// ISSUE: KT-57105
// DUMP_IR

// simpleCase
open class Base_1 {
    val x: Any?
    val y: Any?

    init {
        this as Derived_1
        x = "O"
        this.y = "O"
    }
}

class Derived_1: Base_1()

// deep hierarchy
open class Base_2 {
    val x: Any?
    val y: Any?

    init {
        this as Impl_2
        x = "K"
        this.y = "K"
    }
}

open class Derived_2: Base_2()

class Impl_2: Derived_2()

fun box(): String {
    val a = Derived_1()
    val b = Impl_2()

    val res1 = "" + a.x + b.x
    val res2 = "" + a.y + b.y

    if (res1 != "OK") return "Fail implicit: $res1"
    if (res2 != "OK") return "Fail explicit: $res2"
    return "OK"
}
