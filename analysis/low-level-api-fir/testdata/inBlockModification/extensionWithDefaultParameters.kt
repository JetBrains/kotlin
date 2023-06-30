class MyProducer {
    fun produce(): Int = 4
}

fun MyProducer.test<caret>Fun(param1: Int = produce()) {

}
