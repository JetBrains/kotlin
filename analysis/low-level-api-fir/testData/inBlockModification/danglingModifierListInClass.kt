annotation class Anno(val value: Int)

class Owner {
    fun member() {}

    @Anno(<expr>1 + 2</expr>)
}
