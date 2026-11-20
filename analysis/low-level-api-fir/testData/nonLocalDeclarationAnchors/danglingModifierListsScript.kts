annotation class Anno(val value: Int = 0)

class Owner {
    fun member() {}

    @Anno(1 + 2)
}

@Anno
