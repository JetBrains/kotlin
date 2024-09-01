// LANGUAGE: +AllowSuperCallToJavaInterface
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_IR_AGAINST_OLD, JVM_MULTI_MODULE_OLD_AGAINST_IR
// MODULE: lib
// JVM_DEFAULT_MODE: all
// FILE: Base.kt
interface Base {
    fun f(): Int = 4
    var p: Int
        get() = storage
        set(value) { storage = value }
}

private var storage = 0

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: Derived.kt
interface Derived : Base {
    fun ff(): Int = super.f()
    var pp: Int
        get() = super.p
        set(value) { super.p = value }
}
class DerivedImpl : Derived

// FILE: WithOverride.kt
interface WithOverride : Base {
    override fun f(): Int = 5
    override var p: Int
        get() = storage
        set(value) { storage = value }

    val base_p: Int get() = super.p
}
private var storage = 6
interface DerivedWithOverride : WithOverride {
    fun ff(): Int = super.f()
    var pp: Int
        get() = super.p
        set(value) { super.p = value }
}
class DerivedWithOverrideImpl : DerivedWithOverride

// FILE: Mid.kt
interface Mid : Base
interface DerivedWithMid : Mid {
    fun ff(): Int = super.f()
    var pp: Int
        get() = super.p
        set(value) { super.p = value }
}
class DerivedWithMidImpl : DerivedWithMid

// FILE: box.kt
fun box(): String {
    val derived = DerivedImpl()
    if (derived.ff() != 4) return "Fail DerivedImpl.ff"
    derived.pp = 8
    if (derived.pp != 8) return "Fail DerivedImpl.pp"

    val derivedWithOverride = DerivedWithOverrideImpl()
    if (derivedWithOverride.ff() != 5) return "Fail DerivedWithOverrideImpl.ff"
    derivedWithOverride.pp = 9
    if (derivedWithOverride.pp != 9) return "Fail DerivedWithOverrideImpl.pp"
    if (derivedWithOverride.base_p != 8) return "Fail DerivedWithOverrideImpl.base_p"

    val derivedWithMid = DerivedWithMidImpl()
    if (derivedWithMid.ff() != 4) return "Fail DerivedWithMid.ff"
    derivedWithMid.pp = 10
    if (derivedWithMid.pp != 10) return "Fail DerivedWithMid.pp"

    return "OK"
}
