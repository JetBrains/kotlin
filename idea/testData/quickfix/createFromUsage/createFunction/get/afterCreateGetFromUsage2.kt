// "Create function 'get' from usage" "true"
fun x (y: Any) {
    val z: Any = y[""]
}

fun Any.get(s: String): Any {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}
