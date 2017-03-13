// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

class B<M>

interface A<T, Y : B<T>> {

    fun <T, L> p(p: T): T {
        return p
    }

    val <T> T.z : T?
        get() = null
}


fun box(): String {
    val defaultImpls = Class.forName("A\$DefaultImpls")
    val declaredMethod = defaultImpls.getDeclaredMethod("p", A::class.java, Any::class.java)
    if (declaredMethod.toGenericString() != "public static <T_I1,Y,T,L> T A\$DefaultImpls.p(A<T_I1, Y>,T)") return "fail 1: ${declaredMethod.toGenericString()}"

    val declaredProperty = defaultImpls.getDeclaredMethod("getZ", A::class.java, Any::class.java)
    if (declaredProperty.toGenericString() != "public static <T_I1,Y,T> T A\$DefaultImpls.getZ(A<T_I1, Y>,T)") return "fail 2: ${declaredProperty.toGenericString()}"

    return "OK"
}
