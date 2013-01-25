
fun foo(i : Int?, a : Any?) {
    i?.plus(1)
    if (i != null) {
        i + 1
        if (a is String) {
            a[0]
        }
   }
}

fun box () : String {
    foo(2, "239")
    return "OK"
}
