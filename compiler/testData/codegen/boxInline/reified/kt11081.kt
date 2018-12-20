// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

open class TypeRef<T> {
    val type = target()

    private fun target(): String {
        val thisClass = this.javaClass
        val superClass = thisClass.genericSuperclass

        return superClass.toString()
    }
}



inline fun <reified T> typeWithMessage(message: String = "Hello"): String {
    val type = object : TypeRef<T>() {}
    val target = type.type

    return message + " " + target
}

// FILE: 2.kt

import test.*

fun specifyOptionalArgument() = typeWithMessage<List<Int>>("Hello")

fun useDefault() = typeWithMessage<List<Int>>()

fun box(): String {
    val specifyOptionalArgument = specifyOptionalArgument()
    val useDefault = useDefault()

    if (useDefault != specifyOptionalArgument) return "fail: $useDefault != $specifyOptionalArgument"

    val type = typeWithMessage<List<Int>>("")
    if (type != " test.TypeRef<java.util.List<? extends java.lang.Integer>>") return "fail 2: $type"

    return "OK"
}
