fun typeName(a: Any?) : String {
    return when(a) {
        is ArrayList<*> -> "array list"
        else -> "no idea"
    }
}

fun box() : String {
    if(typeName(ArrayList<Int>()) != "array list") return "array list failed"
    return "OK"
}
