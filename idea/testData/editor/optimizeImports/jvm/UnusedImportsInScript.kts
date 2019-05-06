import kotlin.collections.filter
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.HashMap
import kotlin.text.Charsets
import kotlin.system.measureTimeMillis

class Action {
    fun test() {
        val chs = Charsets.UTF8
        measureTimeMillis({ println(HashMap<String, Int>().size) })
        val test : ArrayList<Int>? = null
    }
}
