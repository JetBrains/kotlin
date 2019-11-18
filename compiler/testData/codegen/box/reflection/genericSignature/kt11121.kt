// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
package test

class B<M>

interface A<T, Y : B<T>> {

    fun <T, L> p(p: T): T {
        return p
    }

    val <T> T.z : T?
        get() = null
}


fun box(): String {
    val defaultImpls = Class.forName("test.A\$DefaultImpls")
    val declaredMethod = defaultImpls.getDeclaredMethod("p", A::class.java, Any::class.java)
    if (declaredMethod.toGenericString() != "public static <T_I1,Y,T,L> T test.A\$DefaultImpls.p(test.A<T_I1, Y>,T)") return "fail 1: ${declaredMethod.toGenericString()}"

    val declaredProperty = defaultImpls.getDeclaredMethod("getZ", A::class.java, Any::class.java)
    if (declaredProperty.toGenericString() != "public static <T_I1,Y,T> T test.A\$DefaultImpls.getZ(test.A<T_I1, Y>,T)") return "fail 2: ${declaredProperty.toGenericString()}"

    return "OK"
}
