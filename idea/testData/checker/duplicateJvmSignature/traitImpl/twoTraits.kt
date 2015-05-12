interface T1 {
    fun getX() = 1
}

interface T2 {
    val x: Int
        get() = 1
}

class <error>C</error> : T1, T2 {
}