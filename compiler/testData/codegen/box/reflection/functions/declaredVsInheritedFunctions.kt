// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J {
    public void publicMemberJ() {}
    private void privateMemberJ() {}
    public static void publicStaticJ() {}
    private static void privateStaticJ() {}
}

// FILE: J2.java
public class J2 extends J {
    public void publicMemberJ2() {}
    private void privateMemberJ2() {}
    public static void publicStaticJ2() {}
    private static void privateStaticJ2() {}
}

// FILE: K.kt

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

open class K : J2() {
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

    val j2 = J2::class

    check(j2.staticFunctions,
          setOf("publicStaticJ2", "privateStaticJ2", "publicStaticJ"))
    check(j2.declaredFunctions,
          setOf("publicMemberJ2", "privateMemberJ2", "publicStaticJ2", "privateStaticJ2"))
    check(j2.declaredMemberFunctions,
          setOf("publicMemberJ2", "privateMemberJ2"))
    check(j2.declaredMemberExtensionFunctions,
          emptySet())

    check(j2.functions, any + listOf("publicMemberJ", "publicStaticJ") + j2.declaredFunctions.names())
    check(j2.memberFunctions, any + listOf("publicMemberJ") + j2.declaredMemberFunctions.names())
    check(j2.memberExtensionFunctions, emptySet())

    val k = K::class

    check(k.staticFunctions,
          emptySet())
    check(k.declaredFunctions,
          setOf("publicMemberK", "privateMemberK", "publicMemberExtensionK", "privateMemberExtensionK"))
    check(k.declaredMemberFunctions,
          setOf("publicMemberK", "privateMemberK"))
    check(k.declaredMemberExtensionFunctions,
          setOf("publicMemberExtensionK", "privateMemberExtensionK"))

    check(k.memberFunctions, any + setOf("publicMemberJ", "publicMemberJ2") + k.declaredMemberFunctions.names())
    check(k.memberExtensionFunctions, k.declaredMemberExtensionFunctions.names())
    check(k.functions, any + (k.memberFunctions + k.memberExtensionFunctions).names())


    val l = L::class

    check(l.staticFunctions, emptySet())
    check(l.declaredFunctions, emptySet())
    check(l.declaredMemberFunctions, emptySet())
    check(l.declaredMemberExtensionFunctions, emptySet())
    check(l.memberFunctions, any + setOf("publicMemberJ", "publicMemberJ2", "publicMemberK"))
    check(l.memberExtensionFunctions, setOf("publicMemberExtensionK"))
    check(l.functions, any + (l.memberFunctions + l.memberExtensionFunctions).names())

    return "OK"
}
