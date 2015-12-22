// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE
import java.util.ArrayList

class ListOfLists<T>(public val x : ArrayList<ArrayList<T>>)

fun main(args : Array<String>) {
    val a : ArrayList<ArrayList<String>> = ArrayList()
    val b : ListOfLists<String> = ListOfLists(a)
    val c : ListOfLists<*> = b
    val d : ArrayList<ArrayList<*>> = <!TYPE_MISMATCH!>c.x<!>

    c.x checkType { _<ArrayList<out ArrayList<*>>>() }
}
