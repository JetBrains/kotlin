// WITH_STDLIB
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNCHECKED_CAST -UNUSED_PARAMETER -UNUSED_VARIABLE -OPT_IN_USAGE_ERROR -UNUSED_EXPRESSION

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

fun poll0(flag: Boolean) {
    val inv = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.Int>")!>if (flag) { ::bar2 } else { ::foo4 }<!>
    inv()
}

fun poll1(flag: Boolean) {
    val inv = if (flag) { ::bar2 } else { ::foo2 }
    inv()
}

fun poll11(flag: Boolean) {
    val inv = if (flag) { ::bar2 } else { ::foo2 }
    inv()
}

fun poll12(flag: Boolean) {
    val inv = if (flag) { ::<!UNRESOLVED_REFERENCE!>bar3<!> } else { ::<!UNRESOLVED_REFERENCE!>foo3<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll13(flag: Boolean) {
    val inv = if (flag) { ::bar2 } else { ::<!UNRESOLVED_REFERENCE!>foo3<!> }
    inv()
}

fun poll14(flag: Boolean) {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (flag) { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!> } else { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!> }<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll15(flag: Boolean) {
    val inv = if (flag) { ::<!UNRESOLVED_REFERENCE!>bar5<!> } else { ::<!UNRESOLVED_REFERENCE!>foo5<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll16(flag: Boolean) {
    val inv = if (flag) { ::Foo6 } else { ::Foo6 }
    inv()
}

fun poll17(flag: Boolean) {
    val inv = if (flag) { foo7() } else { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> }
    inv
}

fun poll2(flag: Boolean) {
    val inv = when (flag) { true -> ::<!UNRESOLVED_REFERENCE!>bar<!> else -> ::<!UNRESOLVED_REFERENCE!>foo<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll21(flag: Boolean) {
    val inv = when (flag) { true -> ::bar2 else -> ::foo2 }
    inv()
}

fun poll22(flag: Boolean) {
    val inv = when (flag) { true -> ::<!UNRESOLVED_REFERENCE!>bar3<!> else -> ::<!UNRESOLVED_REFERENCE!>foo3<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll23(flag: Boolean) {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>when (flag) { true -> ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!> else -> ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!> }<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll24(flag: Boolean) {
    val inv = when (flag) { true -> ::<!UNRESOLVED_REFERENCE!>bar5<!> else -> ::<!UNRESOLVED_REFERENCE!>foo5<!> }
    inv
}

fun poll25(flag: Boolean) {
    val inv = when (flag) { true -> ::Foo6 else -> ::Foo6 }
    inv
}

fun poll26(flag: Boolean) {
    val inv = when (flag) { true -> ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> false -> foo7() else -> ::Foo7 }
    inv
}

fun poll3(flag: Boolean) {
    val inv = when (flag) { true -> ::<!UNRESOLVED_REFERENCE!>bar<!> false -> ::<!UNRESOLVED_REFERENCE!>foo<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll31(flag: Boolean) {
    val inv = when (flag) { true -> ::bar2 false -> ::foo2 }
    inv()
}

fun poll32(flag: Boolean) {
    val inv = when (flag) { true -> ::<!UNRESOLVED_REFERENCE!>bar3<!> false -> ::<!UNRESOLVED_REFERENCE!>foo3<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll33(flag: Boolean) {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>when (flag) { true -> ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!> false -> ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!> }<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll34(flag: Boolean) {
    val inv = when (flag) { true -> ::<!UNRESOLVED_REFERENCE!>bar5<!> false -> ::<!UNRESOLVED_REFERENCE!>foo5<!> }
    inv
}

fun poll35(flag: Boolean) {
    val inv = when (flag) { true -> ::Foo6 false -> ::Foo6 }
    inv
}

fun poll36(flag: Boolean) {
    val inv = when (flag) { true -> ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> false -> foo7() }
    inv
}

fun poll4() {
    val inv = try { ::<!UNRESOLVED_REFERENCE!>bar<!> } finally { ::<!UNRESOLVED_REFERENCE!>foo<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll41() {
    val inv = try { ::bar2 } finally { ::foo2 }
    inv()
}

fun poll42() {
    val inv = try { ::<!UNRESOLVED_REFERENCE!>bar3<!> } finally { ::<!UNRESOLVED_REFERENCE!>foo3<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll43() {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>try { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!> } finally { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!> }<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll44() {
    val inv = try { ::<!UNRESOLVED_REFERENCE!>bar5<!> } finally { ::<!UNRESOLVED_REFERENCE!>foo5<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll45() {
    val inv = try { ::Foo6 } finally { ::Foo6 }
    inv()
}

fun poll46() {
    val inv = try { foo7() } finally { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> }
    inv
}

fun poll5() {
    val inv = try { ::<!UNRESOLVED_REFERENCE!>bar<!> } catch (e: Exception) { ::<!UNRESOLVED_REFERENCE!>foo<!> } finally { ::<!UNRESOLVED_REFERENCE!>foo<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll51() {
    val inv = try { ::bar2 } catch (e: Exception) { ::foo2 } finally { ::foo2 }
    inv()
}

fun poll52() {
    val inv = try { ::<!UNRESOLVED_REFERENCE!>bar3<!> } catch (e: Exception) { ::<!UNRESOLVED_REFERENCE!>foo3<!> } finally { ::<!UNRESOLVED_REFERENCE!>foo3<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll53() {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>try { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!> } catch (e: Exception) { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!> } finally { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!> }<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll54() {
    val inv = try { ::<!UNRESOLVED_REFERENCE!>bar5<!> } catch (e: Exception) { ::<!UNRESOLVED_REFERENCE!>foo5<!> } finally { ::<!UNRESOLVED_REFERENCE!>foo5<!> }
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll55() {
    val inv = try { ::Foo6 } catch (e: Exception) { ::Foo6 } finally { ::Foo6 }
    inv()
}

fun poll56() {
    val inv = try { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> } catch (e: Exception) { foo7() } finally { foo7() }
    inv
}

fun poll6() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar<!>
    inv
}

fun poll61() {
    val inv = ::bar2
    inv
}

fun poll62() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar3<!>
    inv
}

fun poll63() {
    val inv = ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!>
    inv
}

fun poll64() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar5<!>
    inv
}

fun poll65() {
    val inv = ::Foo6
    inv
}

fun poll66() {
    val inv = ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!>
    inv
}

fun poll7() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll71() {
    val inv = ::bar2<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
    inv()
}

fun poll72() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar3<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
    inv<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun poll73() {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!><!>
    inv
}

fun poll74() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar5<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
    inv
}

fun poll75() {
    val inv = ::Foo6<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
    inv
}

fun poll76() {
    val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!><!>
    inv
}

fun poll8() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar<!> in setOf(::<!UNRESOLVED_REFERENCE!>foo<!>)
    <!UNRESOLVED_REFERENCE!>inv<!>()
}

fun poll81() {
    val inv = ::bar2 <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> setOf(::foo2)
    <!UNRESOLVED_REFERENCE!>inv<!>()
}

fun poll82() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar3<!> in setOf(::<!UNRESOLVED_REFERENCE!>foo3<!>)
    <!UNRESOLVED_REFERENCE!>inv<!>()
}

fun poll83() {
    val inv = ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!> in <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>setOf<!>(::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo4<!>)
    inv
}

fun poll84() {
    val inv = ::<!UNRESOLVED_REFERENCE!>bar5<!> in setOf(::<!UNRESOLVED_REFERENCE!>foo5<!>)
    inv
}

fun poll85() {
    val inv = ::Foo6 in setOf(::Foo6)
    inv
}

fun poll86() {
    val inv = ::Foo7 in <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>setOf<!>(::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!>)
    inv
}

fun poll87() {
    val inv = ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>in<!> setOf(foo7())
    inv
}

fun poll88() {
    val inv = foo7() in <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>setOf<!>(::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!>)
    inv
}
