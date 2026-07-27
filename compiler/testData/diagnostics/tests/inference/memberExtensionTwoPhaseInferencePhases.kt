// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

class Box<out T>(val value: T)
class Source<out T>
class Sink<T>

fun <T> Box<T>.receiverOnly(): T = value

fun <T : CharSequence> Box<T>.boundedReceiver(): Int = value.length

fun <T, R> Box<T>.needsArgument(argument: R): R = argument

fun <T, R> Source<T>.combine(sink: Sink<R>) {}

fun <T> Source<T>.reject(sink: Sink<T>) {}

fun <T, R> Box<T>.transform(transform: (T) -> R): R = transform(value)

fun <T> Box<T>.withDefault(value: Int = 0) {}

fun <C, R> C.linkedByBound(defaultValue: () -> R): R where C : R = defaultValue()

operator fun <T> Box<T>.plus(other: Box<T>): Box<T> = this

infix fun <T> Box<T>.merge(other: Box<T>) {}

fun stringLength(value: String): Int = value.length

fun testReceiverPhase(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"receiverOnly\", \"signature\":\"<T> Box<T>.()\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>receiverOnly<!>()
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"boundedReceiver\", \"signature\":\"<T : CharSequence> Box<T>.()\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>boundedReceiver<!>()
}

fun testArgumentPhase(box: Box<String>, source: Source<String>, sink: Sink<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"needsArgument\", \"signature\":\"<T, R> Box<T>.(R)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>needsArgument<!>(42)
    source.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"combine\", \"signature\":\"<T, R> Source<T>.(Sink<R>)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\", \"R\":\"String\"}, \"receiver\":{\"actualType\":\"Source<String>\", \"inferredType\":\"Source<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>combine<!>(sink)
}

fun testArgumentPhaseFailure(source: Source<String>, sink: Sink<Int>) {
    source.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"reject\", \"signature\":\"<T> Source<T>.(Sink<T>)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Source<String>\", \"inferredType\":\"Source<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>reject<!>(<!ARGUMENT_TYPE_MISMATCH("Sink<Int>; Sink<String>")!>sink<!>)
}

fun testPostponedArguments(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"transform\", \"signature\":\"<T, R> Box<T>.((T) -> R)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>transform<!> { it.length }
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"transform\", \"signature\":\"<T, R> Box<T>.((T) -> R)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>transform<!>(::stringLength)
    "".<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"linkedByBound\", \"signature\":\"<C : R, R> C.(() -> R)\", \"normalInference\":{\"inferredTypes\":{\"C\":\"String\", \"R\":\"String?\"}, \"receiver\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"C\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"C\":\"String\", \"R\":\"String?\"}, \"receiverPhaseFixed\":[\"C\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[\"R\"]}}")!>linkedByBound<!> { null }
}

fun testUnsupportedAnalysis(box: Box<String>) {
    box.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"withDefault\", \"signature\":\"<T> Box<T>.(Int)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"error\", \"reason\":\"default, missing, or extra arguments\"}}")!>withDefault<!>()
}

fun testOperatorAndInfixCalls(box: Box<String>) {
    box <!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"plus\", \"signature\":\"<T> Box<T>.(Box<T>)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>+<!> box
    box <!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"merge\", \"signature\":\"<T> Box<T>.(Box<T>)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Box<String>\", \"inferredType\":\"Box<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>merge<!> box
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, classDeclaration, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, integerLiteral, lambdaLiteral, nullableType, operator, out,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, typeConstraint, typeParameter */
