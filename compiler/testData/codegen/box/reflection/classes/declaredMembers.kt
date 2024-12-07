// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: I.java

public class I {
    public static void publicStaticI() {}
    public void publicMemberI() {}
    private static void privateStaticI() {}
    private void privateMemberI() {}
}

// FILE: J.java

public class J extends I {
    public static void publicStaticJ() {}
    public void publicMemberJ() {}
    private static void privateStaticJ() {}
    private void privateMemberJ() {}
}

// FILE: K.kt

import kotlin.reflect.full.declaredMembers
import kotlin.test.assertEquals

open class K : J() {
    open fun publicKFun() {}
    private fun privateKFun() {}
    var publicKProp = Unit
    private val privateKProp = Unit
}

open class L : K() {
    fun publicLFun() {}
    private fun privateLFun() {}
    val publicLProp = Unit
    private var privateLProp = Unit
}

inline fun <reified T> test(vararg names: String) {
    assertEquals(names.toSet(), T::class.declaredMembers.map { it.name }.toSet())
}

data class D(val x: Int)

open class O<T>(val y: T)
data class DO(val z: Int) : O<Int>(2 * z)

@JvmInline
value class V(val b: Boolean)

fun box(): String {
    class Local : L() {
        fun publicLocalFun() {}
        private fun privateLocalFun() {}
        val publicLocalProp = Unit
        private var privateLocalProp = Unit
    }

    test<I>("publicStaticI", "publicMemberI", "privateStaticI", "privateMemberI")
    test<J>("publicStaticJ", "publicMemberJ", "privateStaticJ", "privateMemberJ")
    test<K>("publicKFun", "privateKFun", "publicKProp", "privateKProp")
    test<L>("publicLFun", "privateLFun", "publicLProp", "privateLProp")
    test<Local>("publicLocalFun", "privateLocalFun", "publicLocalProp", "privateLocalProp")
    test<D>("x", "component1", "copy", "equals", "hashCode", "toString")
    test<DO>("z", "component1", "copy", "equals", "hashCode", "toString")
    test<V>("b", "equals", "hashCode", "toString")

    return "OK"
}
