// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-49700
// WITH_STDLIB

// KT-49700: Unchecked covariance type mismatch in case of extension property

var <T> MutableList<T>.customAccessor: T
    get() = get(0)
    set(value) { add(value) }

fun test() {
    val mutableList: MutableList<Int> = mutableListOf()
    val list: MutableList<out Any> = mutableList
    list.<!SETTER_PROJECTED_OUT!>customAccessor<!> = Any() // Should produce TYPE_MISMATCH but doesn't
}

/* GENERATED_FIR_TAGS: assignment, capturedType, functionDeclaration, getter, integerLiteral, localProperty,
nullableType, outProjection, propertyDeclaration, propertyWithExtensionReceiver, setter, typeParameter */
