import test.*

fun box() : String {
    val o = "O"
    val p = "GOOD"
    val result = doWork {
        val k = "K"
        val s = object : A<String>() {

            val param = p;

            override fun getO(): String {
                return o;
            }

            override fun getK(): String {
                return k;
            }
        }

        s.getO() + s.getK() + s.param
    }

    if (result != "OKGOOD") return "fail $result"

    return "OK"
}

