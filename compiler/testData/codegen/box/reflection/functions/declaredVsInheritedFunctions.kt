// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public void publicMemberJ() {}
    private void privateMemberJ() {}
    public static void publicStaticJ() {}
    private static void privateStaticJ() {}
}

// FILE: K.kt

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

open class K : J() {
    public fun publicMemberK() {}
    private fun privateMemberK() {}
    public fun Any.publicMemberExtensionK() {}
    private fun Any.privateMemberExtensionK() {}
}

class L : K()

fun Collection<KFunction<*>>.names(): Set<String> =
        this.map { it.name }.toSet()

fun check(c: Collection<KFunction<*>>, names: Set<String>) {
    assertEquals(names, c.names())
}

fun box(): String {
    val any = setOf("equals", "hashCode", "toString")

    val j = J::class

    check(j.staticFunctions,
          setOf("publicStaticJ", "privateStaticJ"))
    check(j.declaredFunctions,
          setOf("publicMemberJ", "privateMemberJ", "publicStaticJ", "privateStaticJ"))
    check(j.declaredMemberFunctions,
          setOf("publicMemberJ", "privateMemberJ"))
    check(j.declaredMemberExtensionFunctions,
          emptySet())

    check(j.functions, any + j.declaredFunctions.names())
    check(j.memberFunctions, any + j.declaredMemberFunctions.names())
    check(j.memberExtensionFunctions, emptySet())

    val k = K::class

    check(k.staticFunctions,
          emptySet())
    check(k.declaredFunctions,
          setOf("publicMemberK", "privateMemberK", "publicMemberExtensionK", "privateMemberExtensionK"))
    check(k.declaredMemberFunctions,
          setOf("publicMemberK", "privateMemberK"))
    check(k.declaredMemberExtensionFunctions,
          setOf("publicMemberExtensionK", "privateMemberExtensionK"))

    check(k.memberFunctions, any + setOf("publicMemberJ") + k.declaredMemberFunctions.names())
    check(k.memberExtensionFunctions, k.declaredMemberExtensionFunctions.names())
    check(k.functions, any + (k.memberFunctions + k.memberExtensionFunctions).names())


    val l = L::class

    check(l.staticFunctions, emptySet())
    check(l.declaredFunctions, emptySet())
    check(l.declaredMemberFunctions, emptySet())
    check(l.declaredMemberExtensionFunctions, emptySet())
    check(l.memberFunctions, any + setOf("publicMemberJ", "publicMemberK"))
    check(l.memberExtensionFunctions, setOf("publicMemberExtensionK"))
    check(l.functions, any + (l.memberFunctions + l.memberExtensionFunctions).names())

    return "OK"
}
