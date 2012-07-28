// KT-398 Internal error when property initializes with function

class X<T>() {
    val check = { (a : Any) -> a is T }
}

fun box() : String {
    if(X<String>().check(10)) return "fail"
    if(!X<String>().check("lala")) return "fail"
    return "OK"
}
