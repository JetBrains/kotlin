package test

annotation class Anno(val value: Int)

const val CONSTANT = 1

class BigClass {
    fun member1(): Int = 1

    fun member2(): Int = 2

    fun member3(): Int = 3

    fun member4(): Int = 4

    fun member5(): Int = 5

    fun member6(): Int = 6

    fun member7(): Int = 7

    fun member8(): Int = 8

    fun member9(): Int = 9

    fun member10(): Int = 10

    @Anno(<expr>CONSTANT</expr>)
}
