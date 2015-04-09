fun foo(x : Any): String {
    return if(x is Array<String>) x[0] else "fail"
}

fun box(): String {
    return foo(array("OK"))
}