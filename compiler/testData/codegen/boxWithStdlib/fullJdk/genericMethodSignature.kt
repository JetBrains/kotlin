class Z<T> {}

class TParam {}

class Zout<out T> {}

class Zin<in T> {}

class Params(val methodIndex: Int, val paramClass: Class<*>, val expectedReturnType: String, val expecedParamType: String)

class Test<T, X, in Y>() {

    fun test1(p: T): T? = null

    fun test2(p: Z<T>): Z<T>? = null

    fun test3(p: Z<String>): Z<String>? = null

    fun test4(p: X):  Zout<out String>? = null

    fun test5(p: Y): Zin<in TParam>? = null
}

fun box(): String {
    val clz = javaClass<Test<*, *, *>>()

    val params = listOf(
            Params(1, javaClass<Any>(), "T", "T"),
            Params(2, javaClass<Z<*>>(), "Z<T>", "Z<T>"),
            Params(3, javaClass<Z<*>>(), "Z<java.lang.String>", "Z<java.lang.String>"),
            Params(4, javaClass<Any>(), "Zout<? extends java.lang.String>", "X"),
            Params(5, javaClass<Any>(), "Zin<? super TParam>", "Y")
    )


    var result: String = ""
    for(p in params) {
        val fail = test(clz, p.methodIndex, p.paramClass, p.expectedReturnType, p.expecedParamType)
        if (fail != "OK") {
            result += fail + "\n";
        }
    }

    return if (result.isEmpty()) "OK" else result;

}

fun test(clazz: Class<*>, methodIndex: Int, paramClass: Class<*>, expectedReturn : String, expectedParam : String): String {
    val method = clazz.getDeclaredMethod("test$methodIndex", paramClass)!!;

    if (method.getGenericReturnType().toString() != expectedReturn)
        return "fail$methodIndex: " + method.getGenericReturnType();

    val test1Param = method.getGenericParameterTypes()!![0];

    if (test1Param.toString() != expectedParam)
        return "fail${methodIndex}_param: " + test1Param;

    return "OK"
}
