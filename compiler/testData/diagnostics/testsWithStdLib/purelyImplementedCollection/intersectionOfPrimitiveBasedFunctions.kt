// FULL_JDK
// JVM_TARGET: 1.8
// ISSUE: KT-63243

abstract class MyMap<K : Any, V : Any> : AbstractMutableMap<K, V>()

interface II {
    fun replace(key: Int, value: Int): Int? = null
}

abstract class ZI : MyMap<Int, Int>(), II

interface IS {
    fun replace(key: String, value: String): String? = null
}

abstract <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class ZS<!> : MyMap<String, String>(), IS
