// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters

import kotlin.test.assertEquals
import kotlin.reflect.jvm.kotlinFunction

@JvmInline
value class Z(val value: String) {
    context(c: String)
    fun inlineClassMember(): Z = Z(value + c)

    context(c: String)
    fun String.inlineClassMemberExtension(): Z = Z(value + c + this)
}

class A(val value: String) {
    context(c: String)
    fun classMember(): Z = Z(value + c)

    context(c: String)
    fun String.classMemberExtension(): Z = Z(value + c + this)
}

context(c: String)
fun topLevel(): Z = Z(c)

context(c: String)
fun String.topLevelExtension(): Z = Z(c + this)

fun box(): String {
    val inlineClassMember = Z::class.members.single { it.name == "inlineClassMember" }
    assertEquals(Z("ab"), inlineClassMember.call(Z("a"), "b"))

    val inlineClassMemberExtension = Z::class.members.single { it.name == "inlineClassMemberExtension" }
    assertEquals(Z("cde"), inlineClassMemberExtension.call(Z("c"), "d", "e"))

    val classMember = A::class.members.single { it.name == "classMember" }
    assertEquals(Z("fg"), classMember.call(A("f"), "g"))

    val classMemberExtension = A::class.members.single { it.name == "classMemberExtension" }
    assertEquals(Z("hij"), classMemberExtension.call(A("h"), "i", "j"))

    val topLevel = object {}::class.java.enclosingClass.declaredMethods.single { it.name == "topLevel" }.kotlinFunction!!
    assertEquals(Z("k"), topLevel.call("k"))

    val topLevelExtension = object {}::class.java.enclosingClass.declaredMethods.single { it.name == "topLevelExtension" }.kotlinFunction!!
    assertEquals(Z("lm"), topLevelExtension.call("l", "m"))

    return "OK"
}
