class A(val x: Int) {
    constructor(s: String) : this(s.length)

    init {}

    val y get() = x

    fun member() = object {
        val inner = 1
        fun innerFun() = inner
    }

    companion object {
        const val C = 1
    }
}

enum class E {
    First, Second;

    fun self() = this
}
