inline fun call(s: () -> String): String {
    return s()
}

class A {

    private val prop: String = "O"
        get() = call { field + "K" }

    private val prop2: String = "O"
        get() = call { call { field + "K" } }

    fun test1(): String {
        return prop
    }

    fun test2(): String {
        return prop2
    }
}

//0 access\$