import com.sun.corba.se.impl.util.JDKBridge
import com.sun.org.apache.xpath.internal.operations.And
import java.lang.StringBuilder
import java.net.HttpRetryException
import java.util.ArrayList
import kotlin.util.measureTimeMillis
import java.util.HashMap

class Action {
    fun test() {
        measureTimeMillis({ println(HashMap<String, Int>().size()) })
        val test : ArrayList<Int>? = null
    }
}