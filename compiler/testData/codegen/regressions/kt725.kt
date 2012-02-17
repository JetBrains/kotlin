fun Int?.inc() = this.sure().inc()

public fun box() : String {
    var i : Int? = 10
    val j = i++

    return if(j==10 && 11 == i) "OK" else "fail"
}