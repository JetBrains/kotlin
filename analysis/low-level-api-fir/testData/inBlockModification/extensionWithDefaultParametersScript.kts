class MyProducer {
    fun produce(): Int = 4
}

fun MyProducer.testFun(param1: Int = produce()) {
    <expr>42</expr>
}
