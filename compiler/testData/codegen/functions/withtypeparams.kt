class X<T> () {
    fun getTypeChecker() = { (a : Any) -> a is T }
}

fun box() : String {
    val c = X<String>().getTypeChecker()
    if(c(10)) return "fail"
    if(!c("lala")) return "fail"
    return "OK"
}
