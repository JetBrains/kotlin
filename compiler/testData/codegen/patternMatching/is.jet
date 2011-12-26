fun typeName(a: Any?) : String {
    return when(a) {
        is java.util.ArrayList<*> -> "array list"
        else -> "no idea"
    }
}

fun box() : String {
    if(typeName(java.util.ArrayList<Int> ()) != "array list") return "array list failed"
    return "OK"
}
