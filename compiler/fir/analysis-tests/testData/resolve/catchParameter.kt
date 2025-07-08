// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReifiedTypeInCatchClause
typealias StringType = String

fun <T : Throwable> test() {
    try {

    } catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: NullPointerException = NullPointerException()<!>) {

    }

    try {} catch (<!TYPE_PARAMETER_IN_CATCH_PARAMETER!>e: T<!>) {}

    try {} catch (<!CATCH_PARAMETER_TYPE_MISMATCH!>e: () -> Int<!>) {}

    try {} catch (<!CATCH_PARAMETER_TYPE_MISMATCH!>e: StringType<!>) {}

    try {} catch (<!CATCH_PARAMETER_TYPE_MISMATCH, CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: Int = 5<!>) {}

    try {} catch (e: Throwable) {}
}

inline fun <reified T> anotherTest() {
    try {} catch (<!CATCH_PARAMETER_TYPE_MISMATCH!>e: T<!>) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, localProperty, nullableType, propertyDeclaration,
reified, tryExpression, typeAliasDeclaration, typeConstraint, typeParameter */
