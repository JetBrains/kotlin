data class Simple(val x: Int, var y: String)

data class WithBody(val x: Int) {
    val doubled: Int
        get() = x * 2

    fun combine(other: WithBody): WithBody = WithBody(x + other.x)
}
