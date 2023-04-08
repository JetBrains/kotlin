// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

class A(val OK: Int, val somePropertyWithLongName: String) {
    fun foo() {}
    suspend fun bar() {}
}
val topLevelProp = 1

const val propertyName1 = A::OK.name
const val propertyName2 = A::somePropertyWithLongName.name
const val methodName = A::foo.name
const val suspendMethodName = A::bar.name
const val className = ::A.name
const val topLevelPropName = ::topLevelProp.name

fun box(): String {
    if (propertyName1 != "OK") return "Fail 1"
    if (propertyName2 != "somePropertyWithLongName") return "Fail 2"
    if (methodName != "foo") return "Fail 3.1"
    if (suspendMethodName != "bar") return "Fail 3.2"
    if (className != "<init>") return "Fail 4"
    if (topLevelPropName != "topLevelProp") return "Fail 5"
    return "OK"
}