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

const val propertyName1 = A::OK.name
const val propertyName2 = A::somePropertyWithLongName.name
const val methodName1 = A::foo.name
const val methodName2 = A::A.name
const val extensionFunName = 42::baz.name
const val suspendMethodName = A::bar.name
const val className = ::A.name
const val topLevelPropName = ::topLevelProp.name
const val nameInComplexExpression = A::OK.name + "!"

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
