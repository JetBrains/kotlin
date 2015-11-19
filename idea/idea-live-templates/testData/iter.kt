import java.io.InputStream
import java.util.ArrayList
import java.io.FileInputStream
import java.util.HashSet

class MyClass {
    public var collection : HashSet<Int>? = null
    private var isAlive : Boolean = false

    fun main(args : Array<String>, v : Int, o: Any) {
        var str = ""
        val myList = ArrayList<String>()
        val stream = FileInputStream(".")
        if (o is String) {
            <caret>
        }
    }
}
