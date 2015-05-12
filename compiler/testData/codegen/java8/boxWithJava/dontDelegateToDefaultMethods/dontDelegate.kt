class Child : Test<String> {
    override fun call() : String {
        return "OK"
    }
}
fun box(): String {
    val res = Child().call()
    if (res != "OK") return "fail $res"

    return Child().testDefault("OK")
}
