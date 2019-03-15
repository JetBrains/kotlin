// !WITH_NEW_INFERENCE
// FULL_JDK

class A : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    operator fun contains(<!UNUSED_PARAMETER!>x<!>: Char): Boolean = true
}
class B : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    override fun contains(value: Any?): Boolean {
        return super.contains(value)
    }
}

class C : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    operator override fun contains(value: Any?): Boolean {
        return super.contains(value)
    }
}

fun main() {
    val hm = java.util.concurrent.ConcurrentHashMap<String, Int>()
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> hm
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> hm
    1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> hm
    2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> hm

    hm.contains("")
    hm.contains(1)

    "" in (hm as Map<String, Int>)
    "" !in (hm as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>in<!> (hm as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>!in<!> (hm as Map<String, Int>)

    val a = A()
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> a
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> a
    1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> a
    2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> a

    ' ' in a
    ' ' !in a
    a.contains("")
    a.contains(1)

    "" in (a as Map<String, Int>)
    "" !in (a as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>in<!> (a as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>!in<!> (a as Map<String, Int>)

    val b = B()
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> b
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> b
    1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> b
    2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> b

    b.contains("")
    b.contains(1)

    "" in (b as Map<String, Int>)
    "" !in (b as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>in<!> (b as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>!in<!> (b as Map<String, Int>)

    // Actually, we could've allow calls here because the owner explicitly declared as operator, but semantics is still weird
    val c = C()
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> c
    "" <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> c
    1 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>!in<!> c
    2 <!CONCURRENT_HASH_MAP_CONTAINS_OPERATOR!>in<!> c

    c.contains("")
    c.contains(1)

    "" in (c as Map<String, Int>)
    "" !in (c as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>in<!> (c as Map<String, Int>)
    1 <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>!in<!> (c as Map<String, Int>)
}

