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

val a: String = a
val b: String = inPlaceRun { b }
val c: String = notInPlaceRun { c }

val d: String by simpleDelegate(d)
val e: String by inPlaceDelegate { e }
val f: String by notInPlaceDelegate { f }

<!MUST_BE_INITIALIZED!>val g: Int<!>
val h = 1.also { <!VAL_REASSIGNMENT!>g<!> = 2 }
<!MUST_BE_INITIALIZED!>val i: Int<!>
val j by lazy { <!VAL_REASSIGNMENT!>i<!> = 2; 1 }
val k: Int
    get() {
        <!VAL_REASSIGNMENT!>i<!> = 3
        return i
    }

val l: Comparator<String> = object : Comparator<String> {
    val delegate: Comparator<String> get() = n
    override fun compare(o1: String, o2: String): Int = delegate.compare(o1, o2)
}
val m: Comparator<String> = object : Comparator<String> by n {}
val n: Comparator<String> = Comparator { _, _ -> 0 }

val t: String = z
val u: String = inPlaceRun { z }
val v: String = notInPlaceRun { z }
val w: String by simpleDelegate(z)
val x: String by inPlaceDelegate { z }
val y: String by notInPlaceDelegate { z }
val z: String = "VALUE"
