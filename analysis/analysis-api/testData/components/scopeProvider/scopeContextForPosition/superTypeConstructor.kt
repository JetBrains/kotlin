package test

open class Base(val value: Int) {
    fun baseMember(): Int = value
    val baseProp: Int = value
}

class Child : Base(<expr>10</expr>) {
    fun childMember(): Int = value
    val childProp: Int = value
}