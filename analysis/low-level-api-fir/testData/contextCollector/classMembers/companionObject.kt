package test

open class Base(val value: Int)

class Owner : Base(<expr>CONSTANT</expr>) {
    val property: Int = 1

    fun member(): Int = property

    companion object {
        const val CONSTANT = 42

        fun companionMember(): Int = CONSTANT
    }
}
