// WITH_RUNTIME
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNCHECKED_CAST -UNUSED_PARAMETER -UNUSED_VARIABLE -EXPERIMENTAL_API_USAGE_ERROR -UNUSED_EXPRESSION

import kotlin.experimental.ExperimentalTypeInference

fun <K> K.bar3(): K = null as K
fun <K> K.foo3(): K = null as K

fun bar2(): Int = 1
fun foo2(): Float = 1f

fun <K> bar4(): K = null as K
fun <K> foo4(): K = null as K

class Foo6

class Foo7<T>
fun foo7() = null as Foo7<Int>

fun poll1(flag: Boolean) {
    val inv = if (flag) { ::bar2 } else { ::foo2 }
    inv()
}

fun poll11(flag: Boolean) {
    val inv = if (flag) { ::bar2 } else { ::foo2 }
    inv()
}

fun poll12(flag: Boolean) {
    val inv = if (flag) { ::bar3 } else { ::foo3 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll13(flag: Boolean) {
    val inv = if (flag) { ::bar2 } else { ::foo3 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll14(flag: Boolean) {
    val inv = if (flag) { ::bar4 } else { ::foo4 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll15(flag: Boolean) {
    val inv = if (flag) { ::bar5 } else { ::foo5 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll16(flag: Boolean) {
    val inv = if (flag) { ::Foo6 } else { ::Foo6 }
    inv()
}

fun poll17(flag: Boolean) {
    val inv = if (flag) { foo7() } else { ::Foo7 }
    inv
}

fun poll2(flag: Boolean) {
    val inv = when (flag) { true -> ::bar else -> ::foo }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll21(flag: Boolean) {
    val inv = when (flag) { true -> ::bar2 else -> ::foo2 }
    inv()
}

fun poll22(flag: Boolean) {
    val inv = when (flag) { true -> ::bar3 else -> ::foo3 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll23(flag: Boolean) {
    val inv = when (flag) { true -> ::bar4 else -> ::foo4 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll24(flag: Boolean) {
    val inv = when (flag) { true -> ::bar5 else -> ::foo5 }
    inv
}

fun poll25(flag: Boolean) {
    val inv = when (flag) { true -> ::Foo6 else -> ::Foo6 }
    inv
}

fun poll26(flag: Boolean) {
    val inv = when (flag) { true -> ::Foo7 false -> foo7() else -> ::Foo7 }
    inv
}

fun poll3(flag: Boolean) {
    val inv = when (flag) { true -> ::bar false -> ::foo }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll31(flag: Boolean) {
    val inv = when (flag) { true -> ::bar2 false -> ::foo2 }
    inv()
}

fun poll32(flag: Boolean) {
    val inv = when (flag) { true -> ::bar3 false -> ::foo3 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll33(flag: Boolean) {
    val inv = when (flag) { true -> ::bar4 false -> ::foo4 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll34(flag: Boolean) {
    val inv = when (flag) { true -> ::bar5 false -> ::foo5 }
    inv
}

fun poll35(flag: Boolean) {
    val inv = when (flag) { true -> ::Foo6 false -> ::Foo6 }
    inv
}

fun poll36(flag: Boolean) {
    val inv = when (flag) { true -> ::Foo7 false -> foo7() }
    inv
}

fun poll4() {
    val inv = try { ::bar } finally { <!UNRESOLVED_REFERENCE!>::foo<!> }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll41() {
    val inv = try { ::bar2 } finally { ::foo2 }
    inv()
}

fun poll42() {
    val inv = try { ::bar3 } finally { <!UNRESOLVED_REFERENCE!>::foo3<!> }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll43() {
    val inv = try { ::bar4 } finally { ::foo4 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll44() {
    val inv = try { ::bar5 } finally { <!UNRESOLVED_REFERENCE!>::foo5<!> }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll45() {
    val inv = try { ::Foo6 } finally { ::Foo6 }
    inv()
}

fun poll46() {
    val inv = try { foo7() } finally { ::Foo7 }
    inv
}

fun poll5() {
    val inv = try { ::bar } catch (e: Exception) { ::foo } finally { <!UNRESOLVED_REFERENCE!>::foo<!> }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll51() {
    val inv = try { ::bar2 } catch (e: Exception) { ::foo2 } finally { ::foo2 }
    inv()
}

fun poll52() {
    val inv = try { ::bar3 } catch (e: Exception) { ::foo3 } finally { <!UNRESOLVED_REFERENCE!>::foo3<!> }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll53() {
    val inv = try { ::bar4 } catch (e: Exception) { ::foo4 } finally { ::foo4 }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll54() {
    val inv = try { ::bar5 } catch (e: Exception) { ::foo5 } finally { <!UNRESOLVED_REFERENCE!>::foo5<!> }
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll55() {
    val inv = try { ::Foo6 } catch (e: Exception) { ::Foo6 } finally { ::Foo6 }
    inv()
}

fun poll56() {
    val inv = try { ::Foo7 } catch (e: Exception) { foo7() } finally { foo7() }
    inv
}

fun poll6() {
    val inv = <!UNRESOLVED_REFERENCE!>::bar<!>
    inv
}

fun poll61() {
    val inv = ::bar2
    inv
}

fun poll62() {
    val inv = <!UNRESOLVED_REFERENCE!>::bar3<!>
    inv
}

fun poll63() {
    val inv = ::bar4
    inv
}

fun poll64() {
    val inv = <!UNRESOLVED_REFERENCE!>::bar5<!>
    inv
}

fun poll65() {
    val inv = ::Foo6
    inv
}

fun poll66() {
    val inv = ::Foo7
    inv
}

fun poll7() {
    val inv = ::bar!!
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll71() {
    val inv = ::bar2!!
    inv()
}

fun poll72() {
    val inv = ::bar3!!
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll73() {
    val inv = ::bar4!!
    inv
}

fun poll74() {
    val inv = ::bar5!!
    inv
}

fun poll75() {
    val inv = ::Foo6!!
    inv
}

fun poll76() {
    val inv = ::Foo7!!
    inv
}

fun poll8() {
    val inv = <!UNRESOLVED_REFERENCE!>::bar<!> <!NONE_APPLICABLE!>in<!> <!NONE_APPLICABLE!>setOf<!>(<!UNRESOLVED_REFERENCE!>::foo<!>)
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll81() {
    val inv = ::bar2 in setOf(::foo2)
    <!UNRESOLVED_REFERENCE!>inv<!>()
}

fun poll82() {
    val inv = <!UNRESOLVED_REFERENCE!>::bar3<!> <!NONE_APPLICABLE!>in<!> <!NONE_APPLICABLE!>setOf<!>(<!UNRESOLVED_REFERENCE!>::foo3<!>)
    <!INAPPLICABLE_CANDIDATE!>inv<!>()
}

fun poll83() {
    val inv = ::bar4 in setOf(::foo4)
    inv
}

fun poll84() {
    val inv = <!UNRESOLVED_REFERENCE!>::bar5<!> <!NONE_APPLICABLE!>in<!> <!NONE_APPLICABLE!>setOf<!>(<!UNRESOLVED_REFERENCE!>::foo5<!>)
    inv
}

fun poll85() {
    val inv = ::Foo6 in setOf(::Foo6)
    inv
}

fun poll86() {
    val inv = ::Foo7 in setOf(::Foo7)
    inv
}

fun poll87() {
    val inv = ::Foo7 in setOf(foo7())
    inv
}

fun poll88() {
    val inv = foo7() in setOf(::Foo7)
    inv
}
