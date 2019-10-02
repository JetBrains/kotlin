interface MyInterface {
    fun blaBlaBla(bla: Int): String
}

class Implemented : MyInterface {
    override fun blaBlaBla(bla: Int): String {
        var result: String = ""
        for (i in 0..bla) {
            result += "_${i}"
        }
        return result
    }
}

fun box(): String {
    val implemented = Implemented()
    val res = implemented.blaBlaBla(5)
    when (res) {
        "_0_1_2_3_4_5" -> return "OK"
        else -> return "FAIL"
    }
}