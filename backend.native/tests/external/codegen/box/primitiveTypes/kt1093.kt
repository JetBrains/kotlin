val f : (Any) -> String = { it.toString() }

fun box() : String {
    if(!(f === f)) return "fail 1"
    if(!(f == f)) return "fail 2"
    if(!(f.equals(f))) return "fail 3"
    return "OK"
}
