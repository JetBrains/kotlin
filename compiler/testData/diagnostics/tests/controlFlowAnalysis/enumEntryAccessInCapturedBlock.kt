// LANGUAGE: +ProperUninitializedEnumEntryAccessAnalysis
// ISSUE: KT-68451
// WITH_STDLIB

import kotlin.contracts.*
import kotlin.properties.ReadOnlyProperty

enum class Some(val s: String, val s2: String) {
    A(A.someString, <!UNINITIALIZED_ENUM_ENTRY!>B<!>.someString) {
        val a_inner = B.s // NPE
        val b_inner = capture { B.s } // potential NPE
        val c_inner = inPlace { B.s } // NPE
        val d_inner by captureDelegate { B.s } // potential NPE
        val e_inner by inPlaceDelegate { B.s } // NPE
        val h_inner by lazy { when { else -> B.s } }
    },
    B(A.someString, B.someString) {
        val a_inner = B.s.length // potential NPE
        val b_inner = capture { B.s.length } // potential NPE
        val c_inner = inPlace { B.s.length } // potential NPE
        val d_inner by captureDelegate { B.s.length } // potential NPE
        val e_inner by inPlaceDelegate { B.s.length } // potential NPE
        val h_inner by lazy { when { else -> B.s.length } }
    },
    C(A.someString, B.someString) { // OK
        val a_inner = B.s.length // OK
        val b_inner = capture { B.s.length } // OK
        val c_inner = inPlace { B.s.length } // OK
        val d_inner by captureDelegate { B.s.length } // OK
        val e_inner by inPlaceDelegate { B.s.length } // OK
        val h_inner by lazy { when { else -> B.s.length } }
    }
    ;

    val a = <!UNINITIALIZED_ENUM_ENTRY!>A<!>.s // NPE
    val b = capture { A.s } // potential NPE
    val c = inPlace { A.s } // NPE
    val d by captureDelegate { A.s } // potential NPE
    val e by inPlaceDelegate { A.s } // NPE

    val someString: String
        get() = "hello"
}

fun capture(block: () -> Unit): String {
    block()
    return "Capture"
}

@OptIn(ExperimentalContracts::class)
inline fun inPlace(block: () -> Unit): String {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
    return "InPlace"
}

fun captureDelegate(block: () -> Unit): ReadOnlyProperty<Any?, String> {
    block()
    return ReadOnlyProperty { _, _ -> "captureDelegate" }
}

@OptIn(ExperimentalContracts::class)
fun inPlaceDelegate(block: () -> Unit): ReadOnlyProperty<Any?, String> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
    return ReadOnlyProperty { _, _ -> "inPlaceDelegate" }
}
