//KT-597 Type inference failed

fun <T> Array<T>?.get(i: Int) : T {
    if (this != null)
        return this.get(i) // <- inferred type is Any? but &T was excepted
    else throw NullPointerException()
}

operator fun Int?.inc() : Int {
    if (this != null)
        return this.inc()
    else
        throw NullPointerException()
}

fun test() {
   var i : Int? = 10
   var i_inc = i++ // <- expected Int?, but returns Any?
}
