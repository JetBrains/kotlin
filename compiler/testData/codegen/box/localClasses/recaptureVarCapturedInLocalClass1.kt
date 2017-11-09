fun box(): String {
    var x = ""

    class CapturesX {
        override fun toString() = x
    }

    fun localFun() = CapturesX()

    x = "OK"
    return localFun().toString()
}