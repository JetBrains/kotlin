// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

class Box<T>(val value: T)
class Source<T>
class Sink<T>
class Covariant<out T>

fun <T> Box<T>.receiverOnly(): T = value

fun Box<String>.noTypeParameters() {}

fun <T> Box<String>.typeParameterOnlyInArgument(arg: T) {}

fun <T, R> Box<T>.needsArgument(arg: R): R = arg

fun <T, R> Source<T>.combine(sink: Sink<R>) {}

fun <T> Source<T>.reject(sink: Sink<T>) {}

fun <T> T.approximation(argument: T) {}

fun <T, R> Box<T>.transform(transform: (T) -> R): R = transform(value)

fun <T : CharSequence> Box<T>.boundedReceiver(): Int = value.length

fun <C, R> C.linkedByBound(defaultValue: () -> R): R where C : R = defaultValue()

fun <T> Iterable<Covariant<T>>.nestedReceiver(argument: T) {}

fun <T> Array<T>.copyInvariant(fromIndex: Int, toIndex: Int) {}

fun <T, R> Function1<T, R>.functionReceiver() {}

fun <T> projectedArrayReceiver(array: Array<out T>) {
    array.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"copyInvariant\", \"normalInference\":{\"receiver\":{\"actualType\":\"Array<out T>\", \"inferredType\":\"Array<CapturedType(out T)>\", \"relation\":\"under_approximated\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"CapturedType(out T)\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>copyInvariant<!>(0, 0)
}

fun <K, V> MutableMap<K, V>.lookupInvariant(key: K) {}

fun <K, V, M : MutableMap<in K, V>> upperBoundReceiver(destination: M, key: K) {
    destination.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"lookupInvariant\", \"normalInference\":{\"receiver\":{\"actualType\":\"M\", \"inferredType\":\"MutableMap<CapturedType(in K), V>\", \"relation\":\"incomparable\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"K\":\"CapturedType(in K)\", \"V\":\"V\"}, \"receiverPhaseFixed\":[\"K\", \"V\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>lookupInvariant<!>(key)
}

fun stringLength(value: String): Int = value.length

operator fun <T> Box<T>.plus(other: Box<T>): Box<T> = this

infix fun <T> Box<T>.merge(other: Box<T>) {}

fun testAllVariablesFixedFromReceiver(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"receiverOnly\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>receiverOnly<!>()
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"boundedReceiver\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>boundedReceiver<!>()
}

fun testReceiverParameterVariance(function: (String) -> Int) {
    function.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"functionReceiver\", \"normalInference\":{\"receiver\":{\"actualType\":\"(String) -> Int\", \"inferredType\":\"(String) -> Int\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"R\", \"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>functionReceiver<!>()
}

fun testExtensionsWithoutReceiverTypeParametersAreIgnored(box: Box<String>) {
    box.noTypeParameters()
    box.typeParameterOnlyInArgument(42)
}

fun testNotAllVariablesFixedFromReceiver(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"needsArgument\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>needsArgument<!>(42)
}

fun testTwoStageInferenceSucceeds(source: Source<String>, sink: Sink<String>) {
    source.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"combine\", \"normalInference\":{\"receiver\":{\"actualType\":\"Source<String>\", \"inferredType\":\"Source<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>combine<!>(sink)
}

fun testTwoStageInferenceFails(source: Source<String>, sink: Sink<Int>) {
    source.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"reject\", \"normalInference\":{\"receiver\":{\"actualType\":\"Source<String>\", \"inferredType\":\"Source<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>reject<!>(<!ARGUMENT_TYPE_MISMATCH("Sink<Int>; Sink<String>")!>sink<!>)
}

fun testReceiverOverApproximation() {
    "".<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"approximation\", \"normalInference\":{\"receiver\":{\"actualType\":\"String\", \"inferredType\":\"Any\", \"relation\":\"over_approximated\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>approximation<!>(Any())
}

fun testOperatorAndInfixCalls(box: Box<String>) {
    box <!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"plus\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>+<!> box
    box <!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"merge\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>merge<!> box
}

fun testPostponedArguments(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"transform\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>transform<!> { it.length }
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"transform\", \"normalInference\":{\"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>transform<!>(::stringLength)
    "".<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"linkedByBound\", \"normalInference\":{\"receiver\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"C\":\"String\", \"R\":\"String?\"}, \"receiverPhaseFixed\":[\"C\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>linkedByBound<!> { null }
}

fun testNestedReceiverTypeParameterApproximation(values: Set<Covariant<String>>, argument: CharSequence) {
    values.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"nestedReceiver\", \"normalInference\":{\"receiver\":{\"actualType\":\"Set<Covariant<String>>\", \"inferredType\":\"Iterable<Covariant<String>>\", \"relation\":\"over_approximated\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>nestedReceiver<!>("")
    values.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"nestedReceiver\", \"normalInference\":{\"receiver\":{\"actualType\":\"Set<Covariant<String>>\", \"inferredType\":\"Iterable<Covariant<CharSequence>>\", \"relation\":\"over_approximated\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>nestedReceiver<!>(argument)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
