// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-43981

fun <T : Comparable<T>, R : Comparable<R>> ClosedRange<T>.mapBounds(transform: (T) -> R): ClosedRange<R> =
    transform(start) .. transform(endInclusive)

fun <T : Comparable<T>> ClosedRange<T>.mapBounds(transform: (T) -> Int): IntRange =
    transform(start) .. transform(endInclusive)

fun main() {
    val words = "aardvark" .. "zyzzyva"
    val uppercaseWords = words.mapBounds { it.uppercase() } // [TYPE_MISMATCH] Type mismatch. Required: Int. Found: String.
    val wordLengths = words.mapBounds { it.length } // IntRange

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.ranges.ClosedRange<kotlin.String>")!>uppercaseWords<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.ranges.IntRange")!>wordLengths<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, rangeExpression, stringLiteral, typeConstraint, typeParameter */
