// KT-399 Type argument inference not implemented for CALL_EXPRESSION

fun <T> getSameTypeChecker(obj: T) : Function1<Any,Boolean> {
    return { a : Any -> a is T }
}

fun box() : String {
    if(getSameTypeChecker<String>("lala")(10)) return "fail"
    if(!getSameTypeChecker<String>("mama")("lala")) return "fail"
    return "OK"
}