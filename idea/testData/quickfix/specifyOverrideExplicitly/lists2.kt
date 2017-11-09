// "Specify override for 'size: Int' explicitly" "true"
// WITH_RUNTIME

import java.util.*

class <caret>B(private val f: MutableList<String>): ArrayList<String>(), MutableList<String> by f {
    override fun isEmpty(): Boolean {
        return f.isEmpty()
    }
}