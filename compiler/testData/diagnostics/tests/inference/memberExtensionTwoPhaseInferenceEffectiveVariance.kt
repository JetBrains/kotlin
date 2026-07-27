// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

class Invariant<T>
class Covariant<out T>

fun <T> T.directReceiver(argument: T) {}

fun <T, R> Function1<T, R>.functionReceiver() {}

fun <T> Covariant<T>.declarationSiteCovariant() {}

fun <T> Invariant<out T>.useSiteCovariant() {}

fun <T> Invariant<T>.invariantReceiver() {}

fun <T> List<Invariant<T>>.nestedInvariantReceiver() {}

fun Invariant<String>.noTypeParameters() {}

fun <T> Invariant<String>.typeParameterOnlyInArgument(argument: T) {}

fun testEffectiveVariance(
    function: (String) -> Int,
    covariant: Covariant<String>,
    useSiteCovariant: Invariant<out String>,
) {
    "".<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"directReceiver\", \"signature\":\"<T> T.(T)\", \"normalInference\":{\"inferredTypes\":{\"T\":\"Any\"}, \"receiver\":{\"actualType\":\"String\", \"inferredType\":\"Any\", \"relation\":\"over_approximated\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"Any\", \"relation\":\"over_approximated\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"inapplicable\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>directReceiver<!>(Any())
    function.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"functionReceiver\", \"signature\":\"<T, R> (T) -> R.()\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiver\":{\"actualType\":\"(String) -> Int\", \"inferredType\":\"(String) -> Int\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}, \"R\":{\"actualType\":\"Int\", \"inferredType\":\"Int\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"P1\":\"contravariant\", \"R\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\", \"R\":\"Int\"}, \"receiverPhaseFixed\":[\"R\", \"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>functionReceiver<!>()
    covariant.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"declarationSiteCovariant\", \"signature\":\"<T> Covariant<T>.()\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Covariant<String>\", \"inferredType\":\"Covariant<String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":\"String\", \"inferredType\":\"String\", \"relation\":\"exact\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"covariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>declarationSiteCovariant<!>()
    useSiteCovariant.<!MEMBER_EXTENSION_TWO_PHASE_INFERENCE("{\"callableId\":\"useSiteCovariant\", \"signature\":\"<T> Invariant<out T>.()\", \"normalInference\":{\"inferredTypes\":{\"T\":\"String\"}, \"receiver\":{\"actualType\":\"Invariant<out String>\", \"inferredType\":\"Invariant<out String>\", \"relation\":\"exact\"}, \"receiverTypeParameters\":{\"T\":{\"actualType\":null, \"inferredType\":\"String\", \"relation\":\"unavailable\"}}, \"receiverTypeConstructorApproximated\":false, \"receiverParameters\":{\"T\":\"invariant\"}}, \"twoPhaseInference\":{\"result\":\"success\", \"outcome\":\"successful\", \"inferredTypes\":{\"T\":\"String\"}, \"receiverPhaseFixed\":[\"T\"], \"receiverPhaseUnfixed\":[], \"argumentPhaseFixed\":[]}}")!>useSiteCovariant<!>()
}

fun testInvariantReceiversAreIgnored(
    invariant: Invariant<String>,
    nestedInvariant: List<Invariant<String>>,
) {
    invariant.invariantReceiver()
    nestedInvariant.nestedInvariantReceiver()
    invariant.noTypeParameters()
    invariant.typeParameterOnlyInArgument(42)
}

fun <T> testProjectionOnActualReceiverDoesNotAffectEligibility(receiver: Invariant<out T>) {
    receiver.invariantReceiver()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
integerLiteral, nullableType, out, outProjection, stringLiteral, typeParameter */
