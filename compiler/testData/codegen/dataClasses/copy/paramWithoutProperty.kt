data class A(a: Int, b: String) {}

fun box() : String {
    for (method in javaClass<A>().getDeclaredMethods()) {
        if (method.getName() == "copy"){
            val parameterTypes = method.getParameterTypes()
            if (parameterTypes != null && parameterTypes.size == 2) {
                val copy = A(1, "a").copy(a = 2, b = "b")
                return "OK"
            }
            else {
                return "Method copy has " + (if (parameterTypes == null) "0" else parameterTypes.size) + " parameters, expected 2"
            }
        }
    }
    return "fail"
}
