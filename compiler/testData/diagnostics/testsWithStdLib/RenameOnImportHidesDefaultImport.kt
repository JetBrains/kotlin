import kotlin.collections.map as map1
import kotlin.Array as KotlinArray

fun f() {
    listOf(1).map1 { it.hashCode() }
    listOf(1).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>map<!> { <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>hashCode<!>() }
}

fun g(a1: KotlinArray<Int>, a2: <!UNRESOLVED_REFERENCE!>Array<!><Int>){}
