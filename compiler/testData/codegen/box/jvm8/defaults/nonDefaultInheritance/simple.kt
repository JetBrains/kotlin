// TARGET_BACKEND: JVM
// FULL_JDK
// JVM_TARGET: 1.8
// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// MODULE: lib
// JVM_DEFAULT_MODE: all
// FILE: Foo.kt

interface Foo {
    fun toOverride(): String = "fail"
    
    fun nonOverride(): String = "K"
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt

interface Derived : Foo {
    override fun toOverride(): String {
        return "O"
    }
}

class DerivedClass : Derived 

fun box(): String {
    val value = DerivedClass()
    return value.toOverride() + value.nonOverride()
}
