package test

open class Base(val value: Int)

class Child constructor(param: Int) : Base(<expr>param</expr>) {
    val property: Int = 1

    fun member(): Int = property

    class Nested {
        fun nestedMember() = 2
    }
}
