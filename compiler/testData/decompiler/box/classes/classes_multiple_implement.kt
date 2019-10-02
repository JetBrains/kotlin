interface MyInterface {
    fun blaBlaBla(bla: Int): String
}

interface AnotherInterface {
    fun blaBla(bla: Int): String
}


class Implemented : MyInterface, AnotherInterface {
    override fun blaBlaBla(bla: Int): String {
        return bla.toString()
    }

    override fun blaBla(bla: Int): String {
        return bla.unaryMinus().toString()
    }
}

fun box(): String {
    val implemented = Implemented()
    val res = implemented.blaBla(4) + implemented.blaBlaBla(2)
    when (res) {
        "-42" -> return "OK"
        else -> return "FAIL"
    }
}