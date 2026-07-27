// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

class Covariant<out T>

open class Container<out T>
class SpecificContainer : Container<String>()

fun <T> Container<T>.acceptExact(argument: T) {}

fun <T> Covariant<T>.acceptCovariant(argument: T) {}

fun <T> Iterable<Covariant<T>>.nestedReceiver(argument: T) {}

fun testNestedReceiverTypeParameterApproximation(
    values: Set<Covariant<String>>,
    approximatedArgument: CharSequence,
) {
    values.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"nestedReceiver\", \"signature\":\"<T> Iterable<Covariant<T>>.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Set<Covariant<String>>\", \"inferredType\":\"Iterable<Covariant<String>>\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":true, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>nestedReceiver<!>("")
    values.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"nestedReceiver\", \"signature\":\"<T> Iterable<Covariant<T>>.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"CharSequence\"}, \"receiver\":{\"actualType\":\"Set<Covariant<String>>\", \"inferredType\":\"Iterable<Covariant<CharSequence>>\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"CharSequence\", \"relation\":\"over_approximated\"}}, \"receiverTypeConstructorApproximated\":true, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>nestedReceiver<!>(approximatedArgument)
}

fun testReceiverClassOverApproximatedButSuccessful(specific: SpecificContainer) {
    specific.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"acceptExact\", \"signature\":\"<T> Container<T>.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"SpecificContainer\", \"inferredType\":\"Container<String>\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":true, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>acceptExact<!>("")
}

fun testReceiverTypeParameterOverApproximatedButSuccessful(covariant: Covariant<Int>) {
    covariant.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"acceptCovariant\", \"signature\":\"<T> Covariant<T>.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"Number\"}, \"receiver\":{\"actualType\":\"Covariant<Int>\", \"inferredType\":\"Covariant<Number>\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"Int\", \"inferredType\":\"Number\", \"relation\":\"over_approximated\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"Number\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>acceptCovariant<!><Number>(2)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
out, stringLiteral, typeParameter */
