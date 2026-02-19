package test

interface Base {
    fun baseMember(): Int = 10
    val baseProp: Int = 10
}

class Child(base: Base) : Base by <expr>base</expr> {
    fun childMember(): Int = 10
    val childProp: Int = 10
}