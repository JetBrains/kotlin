class A<T>(t: Array<Array<T>>) {
    val a:Array<Array<T>> = t
}

fun box(): String {
    A<Int>(array()) // <- java.lang.VerifyError: (class: A, method: getA signature: ()[[Ljava/lang/Object;) Wrong return type in function
    return "OK"
}
