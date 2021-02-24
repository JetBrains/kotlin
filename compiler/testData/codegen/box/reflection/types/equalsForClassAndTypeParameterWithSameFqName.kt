// TARGET_BACKEND: JVM
// WITH_REFLECT

// FIR incorrectly resolves typeParameterType's return type to the nested class `A.T`.
// IGNORE_BACKEND_FIR: JVM_IR

package test

class A<T> {
    class T

    fun typeParameterType(): T? = null
    fun nestedClassType(): A.T? = null
}

fun box(): String {
    val typeParameterType = A<*>::typeParameterType.returnType
    val classType = A<*>::nestedClassType.returnType

    if (typeParameterType == classType)
        return "Fail 1: type parameter's type constructor shouldn't be equal to the class with the same FQ name"

    if (classType == typeParameterType)
        return "Fail 2: class' type constructor shouldn't be equal to the type parameter with the same FQ name"

    return "OK"
}
