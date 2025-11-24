// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -IntrinsicConstEvaluation

class A(val OK: Int, val somePropertyWithLongName: String) {
    fun foo() {}
    fun A() {}
    suspend fun bar() {}
}
val topLevelProp = 1
fun Int.baz() {}

const val propertyName1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::OK.name<!>
const val propertyName2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::somePropertyWithLongName.name<!>
const val methodName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::foo.name<!>
const val methodName2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::A.name<!>
const val extensionFunName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>42::baz.name<!>
const val suspendMethodName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::bar.name<!>
const val className = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>::A.name<!>
const val topLevelPropName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>::topLevelProp.name<!>
const val nameInComplexExpression = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::OK.name + "!"<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
