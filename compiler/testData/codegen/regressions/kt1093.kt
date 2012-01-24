val f : (Any) -> String = { it.toString() }

fun box() : String {
    if(!(f identityEquals f)) return "fail"
    if(!(f == f)) return "fail"
    if(!(f equals f)) return "fail"
    return "OK"
}
