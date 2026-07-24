// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun unitOrGenericNullable(block: () -> Unit): Int = 1
fun <T> unitOrGenericNullable(block: () -> T?): String = "(2)"

fun unitOrGenericDnn(block: () -> Unit): Int = 1
fun <T> unitOrGenericDnn(block: () -> (T & Any)): String = "(2)"

fun <T> genericUnitOrGenericDnn(block: () -> Unit): Int = 1
fun <T> genericUnitOrGenericDnn(block: () -> (T & Any)): String = "(2)"

val flag = true

fun testUnitOrGenericNullable() {
    val nullResult = unitOrGenericNullable { null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nullResult<!>

    val stringResult = unitOrGenericNullable { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val unitResult = unitOrGenericNullable { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitResult<!>

    val integerResult = unitOrGenericNullable { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>integerResult<!>
}

fun nullableString(): String? = null

fun testUnitOrGenericDnn() {
    val nullResult = unitOrGenericDnn { null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nullResult<!>

    val nullableStringResult = unitOrGenericDnn { if (flag) "" else null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nullableStringResult<!>

    val stringResult = unitOrGenericDnn { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val nothingResult = unitOrGenericDnn { null!! }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>

    val explicitNothingResult = unitOrGenericDnn<Nothing> { null!! }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitNothingResult<!>

    val explicitNullableStringResult = unitOrGenericDnn<String?> { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitNullableStringResult<!>

    val localReturnResult = unitOrGenericDnn {
        if (true) return@unitOrGenericDnn
        nullableString()
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>localReturnResult<!>
}

fun testGenericUnitOrGenericDnn() {
    val explicitNullableStringResult = genericUnitOrGenericDnn<String?> { if (flag) "" else null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitNullableStringResult<!>

    val stringResult = genericUnitOrGenericDnn { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitNothingResult = genericUnitOrGenericDnn<Nothing> { null!! }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitNothingResult<!>

    val explicitStringResult = genericUnitOrGenericDnn<String?> { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitStringResult<!>

    val explicitNullResult = genericUnitOrGenericDnn<String?> { null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitNullResult<!>

    val explicitIntegerResult = genericUnitOrGenericDnn<Nothing> { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitIntegerResult<!>
}

/* GENERATED_FIR_TAGS: checkNotNullCall, dnnType, functionDeclaration, functionalType, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
