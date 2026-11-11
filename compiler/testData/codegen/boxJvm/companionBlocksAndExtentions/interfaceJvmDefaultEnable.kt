// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// JVM_DEFAULT_MODE: enable
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB

// With -jvm-default=enable, DefaultImpls is generated for the default methods of the interface only.
// Companion block functions, including their $default stubs, are static methods of the interface.
package test

interface I {
    fun d(x: Int = 0) = "d$x"

    companion {
        fun withDefault(x: Int = 1) = "default$x"
    }
}

class Impl : I

fun box(): String {
    if (Impl().d() + I.withDefault() != "d0default1") return "Fail"

    val defaultImpls = Class.forName("test.I\$DefaultImpls").declaredMethods.map { it.name }.toSet()
    if (defaultImpls != setOf("d", "d\$default")) return "Fail: DefaultImpls has $defaultImpls"

    return "OK"
}
