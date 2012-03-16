import java.util.ArrayList
import kotlin.util.measureTimeMillis

class Action {
    fun test() {
        measureTimeMillis({ println("Some")})
        val test : ArrayList<Int>? = null
    }
}