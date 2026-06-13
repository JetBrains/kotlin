// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.reflect.KCallable
import kotlin.reflect.full.*
import kotlin.test.assertEquals

open class A {
    fun mfa() {}
    var mpa: String = "a"
    companion {
        fun sfa() {}
        var spa: String = "a"
    }
}

class B : A() {
    fun mfb() {}
    var mpb: String = "b"
    companion {
        fun sfb() {}
        var spb: String = "b"
    }
}

fun check(members: Collection<KCallable<*>>, vararg names: String) {
    assertEquals(names.toSet(), members.map { it.name }.toSet() - "equals" - "hashCode" - "toString")
}

fun box(): String {
    check(A::class.members, "mfa", "mpa", "sfa", "spa")
    check(A::class.declaredMembers, "mfa", "mpa", "sfa", "spa")
    check(A::class.functions, "mfa", "sfa")
    check(A::class.staticFunctions, "sfa")
    check(A::class.memberFunctions, "mfa")
    check(A::class.memberExtensionFunctions)
    check(A::class.declaredFunctions, "mfa", "sfa")
    check(A::class.declaredMemberFunctions, "mfa")
    check(A::class.declaredMemberExtensionFunctions)
    check(A::class.staticProperties, "spa")
    check(A::class.memberProperties, "mpa")
    check(A::class.memberExtensionProperties)
    check(A::class.declaredMemberProperties, "mpa")
    check(A::class.declaredMemberExtensionProperties)

    // Note that there should be no sfa/spa, since statics are not inherited in Kotlin.
    check(B::class.members, "mfa", "mpa", "mfb", "mpb", "sfb", "spb")
    check(B::class.declaredMembers, "mfb", "mpb", "sfb", "spb")
    check(B::class.functions, "mfa", "mfb", "sfb")
    check(B::class.staticFunctions, "sfb")
    check(B::class.memberFunctions, "mfa", "mfb")
    check(B::class.memberExtensionFunctions)
    check(B::class.declaredFunctions, "mfb", "sfb")
    check(B::class.declaredMemberFunctions, "mfb")
    check(B::class.declaredMemberExtensionFunctions)
    check(B::class.staticProperties, "spb")
    check(B::class.memberProperties, "mpa", "mpb")
    check(B::class.memberExtensionProperties)
    check(B::class.declaredMemberProperties, "mpb")
    check(B::class.declaredMemberExtensionProperties)

    return "OK"
}
