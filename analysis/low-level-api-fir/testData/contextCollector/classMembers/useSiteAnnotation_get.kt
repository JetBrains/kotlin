package test

annotation class Anno(val value: Int)

const val CONSTANT = 1

class Owner(@get:Anno(<expr>CONSTANT</expr>) var property: Int) {
    val other: Int = 2

    fun member(): Int = other
}
