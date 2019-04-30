import com.sun.corba.se.impl.util.JDKBridge
import kotlin.collections.filter
import java.lang.StringBuilder
import java.net.HttpRetryException
import java.util.ArrayList
import java.util.HashMap
import kotlin.test.Asserter
import kotlin.Charsets
import kotlin.Assertions
import kotlin.system.measureTimeMillis

class Action {
    fun test() {
        val chs = Charsets.UTF8
        val traait : Asserter? = null
        val objectImport : Assertions? = null
        measureTimeMillis({ println(HashMap<String, Int>().size) })
        val test : ArrayList<Int>? = null
    }
}
