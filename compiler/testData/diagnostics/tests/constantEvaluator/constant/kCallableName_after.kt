// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation

class A(val OK: Int, val somePropertyWithLongName: String) {
    fun foo() {}
    fun A() {}
    suspend fun bar() {}
}
val topLevelProp = 1
fun Int.baz() {}

const val propertyName1 = A::OK.name
const val propertyName2 = A::somePropertyWithLongName.name
const val methodName = A::foo.name
const val methodName2 = A::A.name
const val extensionFunName = 42::baz.name
const val suspendMethodName = A::bar.name
const val className = ::A.name
const val topLevelPropName = ::topLevelProp.name
const val nameInComplexExpression = A::OK.name + "!"

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
