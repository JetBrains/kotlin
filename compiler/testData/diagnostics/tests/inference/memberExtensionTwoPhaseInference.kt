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

fun <T> Box<T>.withDefault(value: Int = 0) {}

fun <T> projectedArrayReceiver(array: Array<out T>) {
    array.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"copyInvariant\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>copyInvariant<!>(0, 0)
}

fun <K, V> MutableMap<K, V>.lookupInvariant(key: K) {}

fun <K, V, M : MutableMap<in K, V>> upperBoundReceiver(destination: M, key: K) {
    destination.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"lookupInvariant\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>lookupInvariant<!>(key)
}

fun stringLength(value: String): Int = value.length

operator fun <T> Box<T>.plus(other: Box<T>): Box<T> = this

infix fun <T> Box<T>.merge(other: Box<T>) {}

fun testAllVariablesFixedFromReceiver(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"receiverOnly\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>receiverOnly<!>()
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"boundedReceiver\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>boundedReceiver<!>()
}

fun testReceiverParameterVariance(function: (String) -> Int) {
    function.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"functionReceiver\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>functionReceiver<!>()
}

fun testExtensionsWithoutReceiverTypeParametersAreIgnored(box: Box<String>) {
    box.noTypeParameters()
    box.typeParameterOnlyInArgument(42)
}

fun testUnsupportedAnalysisIsReported(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"withDefault\", \"signature\":\"<T> Box<T>.(Int)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverParameters\":{\"T\":\"invariant\"}}, \"twoPhaseInference\":{\"result\":\"error\", \"reason\":\"default, missing, or extra arguments\"}}"), MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"withDefault\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":0, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":1}}")!>withDefault<!>()
}

fun testNotAllVariablesFixedFromReceiver(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"needsArgument\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>needsArgument<!>(42)
}

fun testTwoStageInferenceSucceeds(source: Source<String>, sink: Sink<String>) {
    source.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"combine\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>combine<!>(sink)
}

fun testTwoStageInferenceFails(source: Source<String>, sink: Sink<Int>) {
    source.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"reject\", \"signature\":\"<T> Source<T>.(Sink<T>)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Source<String>\", \"inferredType\":\"Source<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverParameters\":{\"T\":\"invariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}"), MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"reject\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":0, \"inapplicableCalls\":1, \"failedCalls\":0, \"errorCalls\":0}}")!>reject<!>(<!ARGUMENT_TYPE_MISMATCH("Sink<Int>; Sink<String>")!>sink<!>)
}

fun testReceiverOverApproximation() {
    "".<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"approximation\", \"signature\":\"<T> T.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"Any\"}, \"receiver\":{\"actualType\":\"String\", \"inferredType\":\"Any\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"Any\", \"relation\":\"over_approximated\"}}, \"receiverParameters\":{}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}"), MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"approximation\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":0, \"inapplicableCalls\":1, \"failedCalls\":0, \"errorCalls\":0}}")!>approximation<!>(Any())
}

fun testOperatorAndInfixCalls(box: Box<String>) {
    box <!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"plus\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>+<!> box
    box <!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"merge\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>merge<!> box
}

fun testPostponedArguments(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"transform\", \"summary\":{\"totalCalls\":2, \"successfulCalls\":2, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>transform<!> { it.length }
    box.transform(::stringLength)
    "".<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"linkedByBound\", \"summary\":{\"totalCalls\":1, \"successfulCalls\":1, \"inapplicableCalls\":0, \"failedCalls\":0, \"errorCalls\":0}}")!>linkedByBound<!> { null }
}

fun testNestedReceiverTypeParameterApproximation(values: Set<Covariant<String>>, argument: CharSequence) {
    values.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY("{\"callableId\":\"nestedReceiver\", \"summary\":{\"totalCalls\":2, \"successfulCalls\":1, \"inapplicableCalls\":1, \"failedCalls\":0, \"errorCalls\":0}}")!>nestedReceiver<!>("")
    values.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"nestedReceiver\", \"signature\":\"<T> Iterable<Covariant<T>>.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"CharSequence\"}, \"receiver\":{\"actualType\":\"Set<Covariant<String>>\", \"inferredType\":\"Iterable<Covariant<CharSequence>>\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"CharSequence\", \"relation\":\"over_approximated\"}}, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>nestedReceiver<!>(argument)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
