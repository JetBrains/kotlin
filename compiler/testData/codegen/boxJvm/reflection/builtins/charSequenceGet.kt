// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

fun box(): String {
    assertEquals(listOf(null, "index"), String::get.parameters.map { it.name })
    assertEquals(Char::class, String::get.returnType.classifier)
    assertEquals("fun kotlin.String.get(kotlin.Int): kotlin.Char", String::get.toString())
    assertEquals('b', String::get.call("abc", 1))
    assertEquals('b', "abc"::get.call(1))
    assertEquals(String::class.member("get"), String::get)
    assertEquals(String::class.member("get").hashCode(), String::get.hashCode())
    assertEquals('b', String::class.member("get").call("abc", 1))

    assertEquals(listOf(null, "index"), CharSequence::get.parameters.map { it.name })
    assertEquals('b', CharSequence::get.call("abc", 1))
    assertEquals(CharSequence::class.member("get"), CharSequence::get)
    assertEquals('b', CharSequence::class.member("get").call("abc", 1))

    assertEquals(StringBuilder::class.member("get"), StringBuilder::get)

    return "OK"
}
