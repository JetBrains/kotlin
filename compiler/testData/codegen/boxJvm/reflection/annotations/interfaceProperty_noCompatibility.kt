// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_DEFAULT_MODE: NO_COMPATIBILITY

import kotlin.reflect.KCallable
import kotlin.test.assertEquals

annotation class Anno(val value: String)

interface A {
    @Anno("member")
    var member: Int

    @Anno("memberExtension")
    val String.memberExtension: Char

    companion object {
        @Anno("companionMember")
        val companionMember: Int
            get() = 42

        @Anno("constVal")
        const val constVal: Int = 42
    }
}

interface B {
    companion object {
        @Anno("jvmField")
        @JvmField
        val jvmField: Unit = Unit
    }
}

private fun check(c: KCallable<*>) {
    assertEquals(c.name, (c.annotations.single { it is Anno } as Anno).value)
}

fun box(): String {
    check(A::member)
    check(A::class.members.single { it.name == "memberExtension" })
    check(A.Companion::companionMember)
    check(A.Companion::constVal)
    check(B.Companion::jvmField)
    return "OK"
}
