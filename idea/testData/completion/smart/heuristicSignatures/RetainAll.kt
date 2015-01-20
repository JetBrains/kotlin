import java.util.*

fun foo(list: ArrayList<String>, p1: ArrayList<Any>, p2: ArrayList<String>) {
    list.retainAll(<caret>)
}

// ABSENT: p1
// EXIST: p2
