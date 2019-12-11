interface ClassData

fun f() = object : ClassData {
    val someInt: Int
        get() {
            return 5
        }
}

fun g() = object : ClassData {
    init {
        if (true) {
            return 0
        }
    }

    fun some(): Int {
        return 6
    }
}