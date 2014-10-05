import java.util.ArrayList

fun foo(): Any {
    val a = ArrayList<String>()
    return a.get(0)
}

fun bar(a: ArrayList<String>) {
}
