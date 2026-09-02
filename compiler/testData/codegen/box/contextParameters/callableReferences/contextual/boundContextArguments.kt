// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalContextParameters
// ISSUE: KT-86452

import kotlin.jvm.internal.CallableReference

context(a: String, b: Int)
fun foo(): String = a + b

class C(val x: String) {
    context(a: String)
    fun bar(): String = x + a
}

context(a: String)
val prop: String get() = a

context(a: String)
var mutableProp: String
    get() = a
    set(value) {}

fun plain(): String = "plain"

fun box(): String {
    val r: () -> String = context("A", 1) { ::foo }
    val rRef = r as CallableReference
    val rArgs = rRef.boundContextArguments
        ?: return "FAIL: boundContextArguments is null for a contextual function reference"
    if (rArgs.size != 2 || rArgs[0] != "A" || rArgs[1] != 1) return "FAIL foo args: ${rArgs.toList()}"
    if (rRef.boundReceiver !== CallableReference.NO_RECEIVER) return "FAIL: unexpected bound receiver: ${rRef.boundReceiver}"

    val c = C("X")
    val rb: () -> String = context("A") { c::bar }
    val rbRef = rb as CallableReference
    val rbArgs = rbRef.boundContextArguments
        ?: return "FAIL: boundContextArguments is null for a bound contextual function reference"
    if (rbArgs.size != 1 || rbArgs[0] != "A") return "FAIL bar args: ${rbArgs.toList()}"
    if (rbRef.boundReceiver !== c) return "FAIL: bound receiver is not the captured instance: ${rbRef.boundReceiver}"

    val rp = context("A") { ::prop } as CallableReference
    val rpArgs = rp.boundContextArguments
        ?: return "FAIL: boundContextArguments is null for a contextual property reference"
    if (rpArgs.size != 1 || rpArgs[0] != "A") return "FAIL prop args: ${rpArgs.toList()}"

    val rm = context("A") { ::mutableProp } as CallableReference
    val rmArgs = rm.boundContextArguments
        ?: return "FAIL: boundContextArguments is null for a contextual mutable property reference"
    if (rmArgs.size != 1 || rmArgs[0] != "A") return "FAIL mutableProp args: ${rmArgs.toList()}"

    if ((::plain as CallableReference).boundContextArguments != null)
        return "FAIL: boundContextArguments is not null for a non-contextual reference"

    return "OK"
}
