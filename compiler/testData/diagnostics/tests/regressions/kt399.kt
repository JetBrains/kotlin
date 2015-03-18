// KT-399 Type argument inference not implemented for CALL_EXPRESSION

fun <T> getSameTypeChecker(<!UNUSED_PARAMETER!>obj<!>: T) : Function1<Any,Boolean> {
    return { a : Any -> a is <!CANNOT_CHECK_FOR_ERASED!>T<!> }
}

fun box() : String {
    if(getSameTypeChecker<String>("lala")(10)) return "fail"
    if(!getSameTypeChecker<String>("mama")("lala")) return "fail"
    return "OK"
}