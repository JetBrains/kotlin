// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -WRONG_INVOCATION_KIND
// WITH_STDLIB

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty

@OptIn(ExperimentalContracts::class)
inline fun <T> inPlaceRun(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

fun <T> notInPlaceRun(block: () -> T): T = null!!

fun <T> simpleDelegate(value: T): ReadOnlyProperty<Any?, T> = null!!

@OptIn(ExperimentalContracts::class)
fun <T> inPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return null!!
}

fun <T> notInPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> = null!!

class Some {
    val a: String = <!UNINITIALIZED_VARIABLE!>a<!>
    val b: String = inPlaceRun { <!UNINITIALIZED_VARIABLE!>b<!> }
    val c: String = notInPlaceRun { c }

    val d: String by simpleDelegate(<!UNINITIALIZED_VARIABLE!>d<!>)
    val e: String by inPlaceDelegate { <!UNINITIALIZED_VARIABLE!>e<!> }
    val f: String by notInPlaceDelegate { f }

    val g: Int
    val h = 1.also { g = 2 }
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val i: Int<!>
    val j by lazy { <!CAPTURED_MEMBER_VAL_INITIALIZATION!>i<!> = 2; 1 }
    val k: Int
        get() {
            <!VAL_REASSIGNMENT!>i<!> = 3
            return i
        }

    val l: Comparator<String> = object : Comparator<String> {
        val delegate: Comparator<String> get() = n
        override fun compare(o1: String, o2: String): Int = delegate.compare(o1, o2)
    }

    val m: Comparator<String> = object : Comparator<String> by <!UNINITIALIZED_VARIABLE!>n<!> {}

    val n: Comparator<String> = Comparator { _, _ -> 0 }

    val t: String = <!UNINITIALIZED_VARIABLE!>z<!>
    val u: String = inPlaceRun { <!UNINITIALIZED_VARIABLE!>z<!> }
    val v: String = notInPlaceRun { z }
    val w: String by simpleDelegate(<!UNINITIALIZED_VARIABLE!>z<!>)
    val x: String by inPlaceDelegate { <!UNINITIALIZED_VARIABLE!>z<!> }
    val y: String by notInPlaceDelegate { z }
    val z: String = "VALUE"

    companion object {
        const val A: String = <!UNINITIALIZED_VARIABLE!>B<!>
        const val B: String = "CONSTANT"

        val tConst: String = Z
        val uConst: String = inPlaceRun { Z }
        val vConst: String = notInPlaceRun { Z }
        val wConst: String by simpleDelegate(Z)
        val xConst: String by inPlaceDelegate { Z }
        val yConst: String by notInPlaceDelegate { Z }
        const val Z: String = "CONSTANT"
    }
}

val a: String = <!UNINITIALIZED_VARIABLE!>a<!>
val b: String = inPlaceRun { <!UNINITIALIZED_VARIABLE!>b<!> }
val c: String = notInPlaceRun { c }

val d: String by simpleDelegate(<!UNINITIALIZED_VARIABLE!>d<!>)
val e: String by inPlaceDelegate { <!UNINITIALIZED_VARIABLE!>e<!> }
val f: String by notInPlaceDelegate { f }

val g: Int
val h = 1.also { g = 2 }
<!MUST_BE_INITIALIZED!>val i: Int<!>
val j by lazy { <!CAPTURED_VAL_INITIALIZATION!>i<!> = 2; 1 }
val k: Int
    get() {
        <!VAL_REASSIGNMENT!>i<!> = 3
        return i
    }

val l: Comparator<String> = object : Comparator<String> {
    val delegate: Comparator<String> get() = n
    override fun compare(o1: String, o2: String): Int = delegate.compare(o1, o2)
}
val m: Comparator<String> = object : Comparator<String> by <!UNINITIALIZED_VARIABLE!>n<!> {}
val n: Comparator<String> = Comparator { _, _ -> 0 }

val t: String = <!UNINITIALIZED_VARIABLE!>z<!>
val u: String = inPlaceRun { <!UNINITIALIZED_VARIABLE!>z<!> }
val v: String = notInPlaceRun { z }
val w: String by simpleDelegate(<!UNINITIALIZED_VARIABLE!>z<!>)
val x: String by inPlaceDelegate { <!UNINITIALIZED_VARIABLE!>z<!> }
val y: String by notInPlaceDelegate { z }
val z: String = "VALUE"

val tConst: String = Z
val uConst: String = inPlaceRun { Z }
val vConst: String = notInPlaceRun { Z }
val wConst: String by simpleDelegate(Z)
val xConst: String by inPlaceDelegate { Z }
val yConst: String by notInPlaceDelegate { Z }
const val Z: String = "CONSTANT"

const val A: String = <!UNINITIALIZED_VARIABLE!>B<!>
const val B: String = "CONSTANT"
