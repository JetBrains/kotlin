// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

package test

open class B

class A {

    companion object {
        @JvmStatic
        fun <T: B> a(s: T) : T {
            return s
        }
    }
}

fun box(): String {
    val method = A::class.java.getDeclaredMethod("a", B::class.java)
    val genericParameterTypes = method.getGenericParameterTypes()

    if (genericParameterTypes.size != 1) return "Wrong number of generic parameters"

    if (genericParameterTypes[0].toString() != "T") return "Wrong parameter type ${genericParameterTypes[0].toString()}"

    if (method.getGenericReturnType().toString() != "T") return "Wrong return type ${method.getGenericReturnType()}"

    return "OK"
}
