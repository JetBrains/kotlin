// FULL_JDK
// STDLIB_JDK8

import java.util.*
import kotlin.streams.toList

fun testStreams(list: ArrayList<String>) {
    list.stream().toList()
}