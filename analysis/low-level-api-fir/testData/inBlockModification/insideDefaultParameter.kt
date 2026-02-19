class MyProducer {
    fun produce(): Int = 4
}

fun MyProducer.testFun(param1: Int = <expr>produce</expr>()) {
    42
}
