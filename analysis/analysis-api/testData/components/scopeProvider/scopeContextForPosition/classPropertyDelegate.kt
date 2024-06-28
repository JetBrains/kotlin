package test

open class Base(val value: Int) {
    fun baseMember(): Int = value
    val baseProp: Int = value
}

class Child(val constructorProp: Int, constructorParam: Int) : Base(10) {
    fun childMember(): Int = value

    val childProp: Int by <expr>::value</expr>
}