class KotlinClass : JavaClass<String>() {
    fun doIt(): String {
        var result = ""
        execute("") {
            result = "OK"
        }
        return result
    }
}

fun box(): String {
    return KotlinClass().doIt()
}
