import test.*

fun box() : String {
    val o = "O"
    return doWork {
        val k = "K"
        val s = object : A<String>() {
            override fun getO(): String {
                return o;
            }

            override fun getK(): String {
                return k;
            }
        }

        s.getO() + s.getK()
    }
}

