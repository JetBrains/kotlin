// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun barRegular(f: () -> Unit) {
    f()
}

inline fun barInline(f: () -> Unit) {
    println("Inside barInline")
    f()
}

fun baz(s: List<String>) {
    println("baz called with: $s")
}

fun bazDeq(s : ArrayDeque<Int>) {}

@OptIn(ExperimentalContracts::class)
fun barWithContract(f: () -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    println("Inside barWithContract")
    f()
}

fun foo() {
    val list1 = mutableListOf("Alice")
    val list2 = mutableListOf(1, 2)

    barRegular {
        baz(list1)
        list1.add("Bob")
        list2.size
    }

    barInline {
        list1.add("Bob")
    }

    barWithContract {
        list1.add("Bob")
    }
}

fun <T> makeMutableList_A4(vararg items: T): MutableList<T> = mutableListOf(*items)
class MyMutableList_A5<T> : MutableList<T> by mutableListOf()
fun <T> faa(t: T, list8: MutableList<T>) {
    val list3 = makeMutableList_A4(1, 2, 3)
    val list4 = MyMutableList_A5<Int>()
    val list5 = list4
    val list6: MutableList<Int>? = mutableListOf(1)
    val list7 = list6!!

    run { list3.clear() }
    barRegular {
        list3.clear()
        list4.indexOf(1)
        list5.add(2)
        list7.add(2)
        list8.add(t)
    }
}

// ------------------------------------------------------------
// SECTION B — MutableSet cases (should log)
// ------------------------------------------------------------

class MyMutableSet_B3<T> : MutableSet<T> by mutableSetOf()
fun sets() {
    val set0 = mutableSetOf(1, 2)
    val set1 = MyMutableSet_B3<Int>()
    barInline { set0.contains(2) }
    barRegular {
        set0.add(3)
        if (set1.contains(1)) {
            set1.add(2)
        }
    }
}

// ------------------------------------------------------------
// SECTION C — MutableMap cases (should log)
// ------------------------------------------------------------

class MyMutableMap_C3<K, V> : MutableMap<K, V> by mutableMapOf()
fun maps() {
    val map0 = mutableMapOf("a" to 1)
    run { map0["a"] }

    val map3 = MyMutableMap_C3<String, Int>()
    barRegular {
        val map1: MutableMap<String, Int>? = mutableMapOf("k" to 2)
        map1?.put("k", 2)
        map3.put("q", 3)
    }
}

// ------------------------------------------------------------
// SECTION D — Other Mutable types (should log)
// ------------------------------------------------------------

fun collections() {
    val dq = ArrayDeque<Int>()
    val hb = HashMap<String, Int>()
    val hs = HashSet<Int>()
    val arr = intArrayOf(1, 2)
    val arr2 = arrayOf(1)

    run { dq.addLast(1) }
    barRegular {
        bazDeq(dq)
        hb.put("a", 1)
        hs.add(1)
        val v = arr.size + arr2.size
    }
}

// ------------------------------------------------------------
// SECTION E — Negative control cases (should NOT log)
// ------------------------------------------------------------

fun negativeControls() {
    var mlVar = mutableListOf(1)
    run { mlVar.add(2) }

    val list13: List<Int> = listOf(1)
    run { list13.size }

    val set0: Set<Int> = setOf(1)
    val map0: Map<String, Int> = mapOf("a" to 1)
    run { set0.contains(1) }
    run { map0["a"] }
}

/* GENERATED_FIR_TAGS: additiveExpression, checkNotNullCall, classDeclaration, classReference, contractCallsEffect,
contracts, functionDeclaration, functionalType, ifExpression, inheritanceDelegation, inline, integerLiteral,
lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration, safeCall, stringLiteral, typeParameter,
vararg */
