import java.util.Collections

fun test() {
    val immutableList = listOf(1, 2)
    Collections.reverse(immutableList)

    val mutableList = mutableListOf(1)
    Collections.reverse(mutableList)
}
