class Clazz {
    default object {
        val a = object {
            fun run(x: String) = x
        }
    }
}

fun box() = "OK"
