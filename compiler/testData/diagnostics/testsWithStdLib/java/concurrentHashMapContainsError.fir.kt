// !LANGUAGE: +ProhibitConcurrentHashMapContains -NonStrictOnlyInputTypesChecks
// !WITH_NEW_INFERENCE
// FULL_JDK

class A : java.util.concurrent.ConcurrentHashMap<String, Int>() {
    operator fun contains(x: Char): Boolean = true
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
    val hm = java.util.concurrent.<!UNRESOLVED_REFERENCE!>ConcurrentHashMap<!><String, Int>()
    "" in hm
    "" !in hm
    1 <!AMBIGUITY, UNRESOLVED_REFERENCE!>!in<!> hm
    2 <!AMBIGUITY!>in<!> hm

    hm.contains("")
    hm.<!AMBIGUITY!>contains<!>(1)

    "" in (hm as Map<String, Int>)
    "" !in (hm as Map<String, Int>)
    1 in (hm as Map<String, Int>)
    1 !in (hm as Map<String, Int>)

    val a = A()
    "" in a
    "" !in a
    1 !in a
    2 in a

    ' ' in a
    ' ' !in a
    a.contains("")
    a.contains(1)

    "" in (a as Map<String, Int>)
    "" !in (a as Map<String, Int>)
    1 in (a as Map<String, Int>)
    1 !in (a as Map<String, Int>)

    val b = B()
    "" in b
    "" !in b
    1 !in b
    2 in b

    b.contains("")
    b.contains(1)

    "" in (b as Map<String, Int>)
    "" !in (b as Map<String, Int>)
    1 in (b as Map<String, Int>)
    1 !in (b as Map<String, Int>)

    // Actually, we could've allow calls here because the owner explicitly declared as operator, but semantics is still weird
    val c = C()
    "" in c
    "" !in c
    1 !in c
    2 in c

    c.contains("")
    c.contains(1)

    "" in (c as Map<String, Int>)
    "" !in (c as Map<String, Int>)
    1 in (c as Map<String, Int>)
    1 !in (c as Map<String, Int>)
}

