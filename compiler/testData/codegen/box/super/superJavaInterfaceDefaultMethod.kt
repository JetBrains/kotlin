// LANGUAGE: +AllowSuperCallToJavaInterface
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// FILE: Base.java
public interface Base {
    default int f() { return 4; }
}

// FILE: Derived.kt
interface Derived : Base {
    fun ff(): Int = super.f()
}
class DerivedImpl : Derived

// FILE: WithOverride.kt
interface WithOverride : Base {
    override fun f(): Int = 5
}
interface DerivedWithOverride : WithOverride {
    fun ff(): Int = super.f()
}
class DerivedWithOverrideImpl : DerivedWithOverride

// FILE: Mid.kt
interface Mid : Base
interface DerivedWithMid : Mid {
    fun ff(): Int = super.f()
}
class DerivedWithMidImpl : DerivedWithMid

// FILE: box.kt
fun box(): String {
    if (DerivedImpl().ff() != 4) return "Fail DerivedImpl"
    if (DerivedWithOverrideImpl().ff() != 5) return "Fail DerivedWithOverrideImpl"
    if (DerivedWithMidImpl().ff() != 4) return "Fail DerivedWithMidImpl"
    return "OK"
}
