package test

import kotlin.platform.platformStatic

open class B

class A {

    default object {
        [platformStatic]
        fun <T: B> a(s: T) : T {
            return s
        }
    }
}

fun box(): String {
    val method = javaClass<A>().getDeclaredMethod("a", javaClass<B>())
    val genericParameterTypes = method.getGenericParameterTypes()

    if (genericParameterTypes.size() != 1) return "Wrong number of generic parameters"

    if (genericParameterTypes[0].toString() != "T") return "Wrong parameter type ${genericParameterTypes[0].toString()}"

    if (method.getGenericReturnType().toString() != "T") return "Wrong return type ${method.getGenericReturnType()}"

    return "OK"
}
