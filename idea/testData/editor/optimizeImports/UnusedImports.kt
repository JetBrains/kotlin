import com.sun.corba.se.impl.util.JDKBridge
import com.sun.org.apache.xpath.internal.operations.And
import java.lang.StringBuilder
import java.net.HttpRetryException
import java.util.ArrayList
import kotlin.util.measureTimeMillis

class Action {
    fun test() {
        measureTimeMillis({ println("Some")})
        val test : ArrayList<Int>? = null
    }
}