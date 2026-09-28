// RUN_PIPELINE_TILL: FRONTEND

const val value = 10

const val simpleTry = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>try { 1 } catch (e: Exception) { 2 }<!>

const val tryWithFinally = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>try { 1 } catch (e: Exception) { 2 } finally { }<!>

const val tryWithMultipleCatches = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>try {
    value
} catch (e: IllegalStateException) {
    1
} catch (e: Exception) {
    2
}<!>

val nonConstFlag = true
const val errorTry = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>try {
    if (nonConstFlag) 1 else 2
} catch (e: Exception) {
    3
}<!>

/* GENERATED_FIR_TAGS: const, ifExpression, integerLiteral, localProperty, propertyDeclaration, tryExpression */
