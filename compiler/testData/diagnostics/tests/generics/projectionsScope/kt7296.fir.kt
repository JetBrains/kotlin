// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE
import java.util.ArrayList

class ListOfLists<T>(public val x : ArrayList<ArrayList<T>>)

fun main() {
    val a : ArrayList<ArrayList<String>> = ArrayList()
    val b : ListOfLists<String> = ListOfLists(a)
    val c : ListOfLists<*> = b
    val d : ArrayList<ArrayList<*>> = c.x

    c.x checkType { <!UNRESOLVED_REFERENCE!>_<!><ArrayList<out ArrayList<*>>>() }
}
