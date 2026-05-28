// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// ISSUES: KT-86176
import kotlin.experimental.ExperimentalTypeInference

suspend fun mySuspend() = ""
fun notSuspend() = ""

val propertyNonSuspendType: () -> String = { "OK" }
val propertySuspendType: suspend () -> Unit = { }

@JvmName("suspendAndSuspend2")
fun suspendAndSuspend(block: suspend () -> String) = 1
fun suspendAndSuspend(block: suspend () -> Unit) = "(2)"

@JvmName("stringSuspendAndUnitNotSuspend2")
fun stringSuspendAndUnitNotSuspend(block: suspend () -> String) = 1
fun stringSuspendAndUnitNotSuspend(block: () -> Unit) = "(2)"

@JvmName("stringNotSuspendAndUnitSuspend2")
fun stringNotSuspendAndUnitSuspend(block: () -> String) = 1
fun stringNotSuspendAndUnitSuspend(block: suspend () -> Unit) = "(2)"

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun suspendNotSuspendWithAnnotation(block: () -> String) = 1
fun suspendNotSuspendWithAnnotation(block: suspend () -> Unit) = "(2)"

fun test() {
    val case1 = suspendAndSuspend { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case1<!>

    val case2 = suspendAndSuspend { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case2<!>

    val case3 = suspendAndSuspend { mySuspend() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case3<!>

    val case4 = suspendAndSuspend { notSuspend() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case4<!>

    val case5 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>suspendAndSuspend<!>(propertyNonSuspendType)
     <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: suspendAndSuspend, [/suspendAndSuspend, /suspendAndSuspend]")!>case5<!>

    val case6 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>suspendAndSuspend<!> { TODO() }

    val case7 = stringSuspendAndUnitNotSuspend { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case7<!>

    val case8 = stringSuspendAndUnitNotSuspend { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case8<!>

    val case9 = stringSuspendAndUnitNotSuspend { mySuspend() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case9<!>

    val case10 = stringSuspendAndUnitNotSuspend { notSuspend() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case10<!>

    val case11 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>stringSuspendAndUnitNotSuspend<!>(propertyNonSuspendType)
     <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: stringSuspendAndUnitNotSuspend, [/stringSuspendAndUnitNotSuspend, /stringSuspendAndUnitNotSuspend]")!>case11<!>

    val case12 = <!NONE_APPLICABLE!>stringSuspendAndUnitNotSuspend<!>(propertySuspendType)

    val case13 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>stringSuspendAndUnitNotSuspend<!> { TODO() }

    val case14 = stringNotSuspendAndUnitSuspend { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case14<!>

    val case15 = stringNotSuspendAndUnitSuspend { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case15<!>

    val case16 = stringNotSuspendAndUnitSuspend { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>mySuspend<!>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case16<!>

    val case17 = stringNotSuspendAndUnitSuspend { notSuspend() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case17<!>

    val case18 = suspendNotSuspendWithAnnotation { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case18<!>           //KT-86176

    val case19 = suspendNotSuspendWithAnnotation { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case19<!>

    val case20 = suspendNotSuspendWithAnnotation { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>mySuspend<!>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case20<!>           //KT-86176
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, functionTypeConversion, functionalType, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration, stringLiteral, suspend */
