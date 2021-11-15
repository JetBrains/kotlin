// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// !JVM_DEFAULT_MODE: all
// FILE: Foo.kt

interface Foo {
    fun toOverride(): List<String> = null!!
    
    fun nonOverride(): List<String> = Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
}

// MODULE: main(lib)
// !JVM_DEFAULT_MODE: disable
// !JVM_DEFAULT_ALLOW_NON_DEFAULT_INHERITANCE
// FILE: main.kt

interface Derived : Foo {
    override fun toOverride() = Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
}

class DerivedClass : Derived 


fun box(): String {
    val override = DerivedClass().toOverride()
    if (override[1] != "Derived\$DefaultImpls.toOverride") return "fail 1: ${override[1]}"
    if (override[2] != "DerivedClass.toOverride") return "fail 2: ${override[2]}"
    if (override[3] != "MainKt.box") return "fail 3: ${override[3]}"

    val nonOverride = DerivedClass().nonOverride()
    if (nonOverride[1] != "Foo.nonOverride") return "fail 3: ${nonOverride[1]}"
    if (nonOverride[2] != "MainKt.box") return "fail 4: ${nonOverride[2]}"

    return "OK"
}
