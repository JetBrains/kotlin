// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM

fun <T> T.id() = this

class A(val OK: Int, val somePropertyWithLongName: String) {
    fun foo() {}
    fun A() {}
    suspend fun bar() {}
}
val topLevelProp = 1
fun Int.baz() {}

const val propertyName1 = A::OK.<!EVALUATED("OK")!>name<!>
const val propertyName2 = A::somePropertyWithLongName.<!EVALUATED("somePropertyWithLongName")!>name<!>
const val methodName1 = A::foo.<!EVALUATED("foo")!>name<!>
const val methodName2 = A::A.<!EVALUATED("A")!>name<!>
const val extensionFunName = 42::baz.<!EVALUATED("baz")!>name<!>
const val suspendMethodName = A::bar.<!EVALUATED("bar")!>name<!>
const val className = ::A.<!EVALUATED("<init>")!>name<!>
const val topLevelPropName = ::topLevelProp.<!EVALUATED("topLevelProp")!>name<!>
const val nameInComplexExpression = <!EVALUATED("OK!")!>A::OK.name + "!"<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (propertyName1.id() != "OK") return "Fail propertyName1"
    if (propertyName2.id() != "somePropertyWithLongName") return "Fail propertyName2"
    if (methodName1.id() != "foo") return "Fail methodName1"
    if (methodName2.id() != "A") return "Fail methodName2"
    if (suspendMethodName.id() != "bar") return "Fail suspendMethondName"
    if (className.id() != "<init>") return "Fail className"
    if (topLevelPropName.id() != "topLevelProp") return "Fail topLevelPropName"
    if (nameInComplexExpression.id() != "OK!") return "Fail nameInComplexExpression"
    return "OK"
}
