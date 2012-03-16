import java.util.ArrayList
import java.util.HashMap
import kotlin.util.measureTimeMillis

class Action {
    fun test() {
        measureTimeMillis({ println(HashMap<String, Int>().size()) })
        val test : ArrayList<Int>? = null
    }
}