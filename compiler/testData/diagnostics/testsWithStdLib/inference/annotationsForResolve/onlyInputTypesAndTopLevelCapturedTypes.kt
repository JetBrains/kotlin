// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !LANGUAGE: -NonStrictOnlyInputTypesChecks
// !WITH_NEW_INFERENCE

class Inv<T>

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes K> Inv<out K>.onlyOut(e: K) {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes K : Number> Inv<out K>.onlyOutUB(e: K) {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes K> Inv<in K>.onlyIn(e: K) {}

fun test(
    invStar: Inv<*>,
    invOut: Inv<out Number>,
    invIn: Inv<in Number>
) {
    invStar.onlyOut("str")
    invOut.onlyOut(42)
    invOut.onlyOut(1L)

    invOut.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES, OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>onlyOutUB<!>(<!NI;TYPE_MISMATCH!>"str"<!>)
    invStar.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>onlyOutUB<!>(0)
    invOut.onlyOutUB(42)
    invOut.onlyOutUB(1L)

    invIn.<!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>onlyIn<!>(<!NI;TYPE_MISMATCH!>"str"<!>)
    invIn.onlyIn(42)
    invIn.onlyIn(1L)
}

// From KT-32157
fun test2(value: Any?) {
    val result = (value as? Map<*, *>)?.get("result")
}

// From KT-32116
fun test3(h: HashMap<*, *>) {
    val a = h["str"]
    val b = h[1]
    val c = h["other"] as? Double
}

// From KT-32218
fun test4() {
    val map: Map<out Any, Any> = mapOf(
        true to true,
        1L to 1L
    )
    val test = map[1L]
}

// From KT-32235

class A<T> {
    val children = mutableListOf<B<T>>()
}

class B<T>

class Test5 {
    var a: A<*>? = null
    var b: B<*>? = null
        set(value) {
            if (value != null) {
                val a = a
                require(a != null && <!DEBUG_INFO_SMARTCAST!>value<!> in <!OI;DEBUG_INFO_SMARTCAST!>a<!>.children)
            }
            field = value
        }
}