import java.util.*

class Template() {
    val collected = ArrayList<String>()

    fun String.plus() {
       collected.add(this@plus)
    }

    fun test() {
        + "239"
    }
}

fun box() : String {
    val u = Template()
    u.test()
    return if(u.collected.size() == 1 && u.collected.get(0) == "239") "OK" else "fail"
}