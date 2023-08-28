// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61384
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

open class A {
    @JvmField public val publicField = "1";
    @JvmField internal val internalField = "2";
    @JvmField protected val protectedfield = "3";
}

open class B : A() {

}

// MODULE: main()(lib)
// FILE: B.kt

open class C : B() {
    fun test(): String {
        return publicField + super.internalField + super.protectedfield
    }
}

fun box(): String {
    C().test()
    return "OK"
}
