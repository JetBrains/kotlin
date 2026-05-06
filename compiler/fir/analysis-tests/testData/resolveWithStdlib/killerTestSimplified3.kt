// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnhancementsOfSecondIncorporationKind25
// ISSUE: KT-66469
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN

interface Inv<E>

operator fun <T : (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9> Inv<T>.invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, args7: A7, args8: A8, args9: A9) {}

fun main(
    x: Inv<(String, String, String, String, String, String, String, String, String) -> String>,
) {
    x("","","","","","","","","")
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration, nullableType,
operator, stringLiteral, typeConstraint, typeParameter */
