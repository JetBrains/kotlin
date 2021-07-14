operator fun Int?.inc() : Int { if (this != null) return this.inc() else throw NullPointerException() }

fun box() : String {
    var i : Int? = 10
    val j = i++

    return if(j==10 && 11 == i) "OK" else "fail"
}
