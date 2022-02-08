// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

open class A(@JvmField public val publicField: String = "1",
             @JvmField internal val internalField: String = "2",
             @JvmField protected val protectedfield: String = "3")

open class B : A()

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
