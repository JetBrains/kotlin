import java.util.ArrayList

fun f() {
    val a = ArrayList<Int>()
    val v = if (true) 1 else 2
    println(a[<caret>v])
}