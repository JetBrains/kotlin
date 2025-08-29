// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM

fun <T> T.id() = this

class A(val OK: Int, val somePropertyWithLongName: String) {
    fun foo() {}
    suspend fun bar() {}
}
val topLevelProp = 1

const val propertyName1 = A::OK.<!EVALUATED{IR}("OK")!>name<!>
const val propertyName2 = A::somePropertyWithLongName.<!EVALUATED{IR}("somePropertyWithLongName")!>name<!>
const val methodName = A::foo.<!EVALUATED{IR}("foo")!>name<!>
const val suspendMethodName = A::bar.<!EVALUATED{IR}("bar")!>name<!>
const val className = ::A.<!EVALUATED{IR}("<init>")!>name<!>
const val topLevelPropName = ::topLevelProp.<!EVALUATED{IR}("topLevelProp")!>name<!>
const val nameInComplexExpression = <!EVALUATED{IR}("OK!")!>A::OK.name + "!"<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (propertyName1.id() != "OK") return "Fail 1"
    if (propertyName2.id() != "somePropertyWithLongName") return "Fail 2"
    if (methodName.id() != "foo") return "Fail 3"
    if (suspendMethodName.id() != "bar") return "Fail 3.2"
    if (className.id() != "<init>") return "Fail 4"
    if (topLevelPropName.id() != "topLevelProp") return "Fail 5"
    if (nameInComplexExpression.id() != "OK!") return "Fail 5"
    return "OK"
}
