class A(val msg: String) {
    init {
        println("init $msg")
    }
    override fun toString(): String = msg
}

val globalValue1 = 1
val globalValue2 = A("globalValue2")
val globalValue3 = A("globalValue3")

fun main(args: Array<String>) {
    println(globalValue1.toString())
    println(globalValue2.toString())
    println(globalValue3.toString())
}

