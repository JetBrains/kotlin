// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters

import kotlin.test.assertEquals
import kotlin.reflect.jvm.kotlinFunction

@JvmInline
value class Z(val value: Int) {
    context(c: Int)
    fun inlineClassMember(): Z = Z(value + c)

    context(c: Int)
    fun Int.inlineClassMemberExtension(): Z = Z(value + c + this)
}

class A(val value: Int) {
    context(c: Int)
    fun classMember(): Z = Z(value + c)

    context(c: Int)
    fun Int.classMemberExtension(): Z = Z(value + c + this)
}

context(c: Int)
fun topLevel(): Z = Z(c)

context(c: Int)
fun Int.topLevelExtension(): Z = Z(c + this)

fun box(): String {
    val inlineClassMember = Z::class.members.single { it.name == "inlineClassMember" }
    assertEquals(Z(12), inlineClassMember.call(Z(10), 2))

    val inlineClassMemberExtension = Z::class.members.single { it.name == "inlineClassMemberExtension" }
    assertEquals(Z(123), inlineClassMemberExtension.call(Z(20), 100, 3))

    val classMember = A::class.members.single { it.name == "classMember" }
    assertEquals(Z(45), classMember.call(A(40), 5))

    val classMemberExtension = A::class.members.single { it.name == "classMemberExtension" }
    assertEquals(Z(456), classMemberExtension.call(A(50), 400, 6))

    val topLevel = object {}::class.java.enclosingClass.declaredMethods.single { it.name == "topLevel" }.kotlinFunction!!
    assertEquals(Z(7), topLevel.call(7))

    val topLevelExtension = object {}::class.java.enclosingClass.declaredMethods.single { it.name == "topLevelExtension" }.kotlinFunction!!
    assertEquals(Z(78), topLevelExtension.call(8, 70))

    return "OK"
}
