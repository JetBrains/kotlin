import com.sun.corba.se.impl.util.JDKBridge
import com.sun.org.apache.xpath.internal.operations.And
import java.lang.StringBuilder
import java.net.HttpRetryException
import java.util.ArrayList
import java.util.HashMap
import kotlin.test.Asserter
import kotlin.text.Charsets
import kotlin.system.measureTimeMillis

class Action {
    fun test() {
        val chs = Charsets.UTF8
        val traait : Asserter? = null
        measureTimeMillis({ println(HashMap<String, Int>().size) })
        val test : ArrayList<Int>? = null
    }
}