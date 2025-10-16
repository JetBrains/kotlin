// TARGET_BACKEND: JVM

// WITH_REFLECT
package test

import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals

data class Box<T>(var member: T) {
    var <S> S.memExt: T
        get() = this as T
        set(value) {}
}

var <U> U.extension: U?
    get() = this
    set(value) {}

fun box(): String {
    val member = Box<String>::member
    assertEquals("var test.Box<T>.member: T", member.toString())
    assertEquals("test.Box<T>", member.parameters.single().type.toString())
    assertEquals("test.Box<T>", member.getter.parameters.single().type.toString())
    assertEquals("test.Box<T>", member.setter.parameters[0].type.toString())
    assertEquals("T", member.setter.parameters[1].type.toString())

    val extension = Any::extension
    assertEquals("var U.extension: U?", extension.toString())
    assertEquals("U", extension.parameters.single().type.toString())
    assertEquals("U", extension.getter.parameters.single().type.toString())
    assertEquals("U", extension.setter.parameters[0].type.toString())
    assertEquals("U?", extension.setter.parameters[1].type.toString())

    val memExt = Box::class.members.single { it.name == "memExt" } as KMutableProperty<*>
    assertEquals("var test.Box<T>.(S.)memExt: T", memExt.toString())
    assertEquals("test.Box<T>", memExt.parameters[0].type.toString())
    assertEquals("S", memExt.parameters[1].type.toString())
    assertEquals("test.Box<T>", memExt.getter.parameters[0].type.toString())
    assertEquals("S", memExt.getter.parameters[1].type.toString())
    assertEquals("test.Box<T>", memExt.setter.parameters[0].type.toString())
    assertEquals("S", memExt.setter.parameters[1].type.toString())
    assertEquals("T", memExt.setter.parameters[2].type.toString())

    return member.call(Box("OK"))
}
