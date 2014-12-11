// !DIAGNOSTICS: -UNUSED_PARAMETER
package kotlin1
import java.util.*

public inline fun <reified T> Array(n: Int, block: (Int) -> T): Array<T> = null!!


fun main(args : Array<String>) {
    val al : ArrayList<Int> = ArrayList<Int>()

    // A type mismatch on this line means that jdk-annotations were not loaded
    al.toArray(Array<Int>(3, {1})) : Array<Int>
}
