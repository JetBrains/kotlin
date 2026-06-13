// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnhancementsOfSecondIncorporationKind25
// ISSUE: KT-66469

interface Inv<E>

operator fun <T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>
    Inv<T>.invoke(
        arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, args7: A7, args8: A8, args9: A9,
        arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18
    ) {}

fun main(
    x: Inv<(String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String) -> String>,
) {
    x("","","","","","","","","","","","","","","","","","")
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration, nullableType,
operator, stringLiteral, typeConstraint, typeParameter */
