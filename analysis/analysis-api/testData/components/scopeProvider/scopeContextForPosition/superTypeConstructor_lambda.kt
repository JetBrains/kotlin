package test

class Receiver {
    fun fromReceiver() {}
}

open class Base(val value: Receiver.() -> Unit) {
    fun baseMember(): Int = 10
    val baseProp: Int = 10
}

class Child : Base({ <expr>this</expr> }) {
    fun childMember(): Int = 10
    val childProp: Int = 10
}