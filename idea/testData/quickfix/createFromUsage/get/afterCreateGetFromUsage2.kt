// "Create method 'get' from usage" "true"
fun x (y: Any) {
    val z: Any = y[""]
}
fun Any.get(s: String): Any {
    throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
}