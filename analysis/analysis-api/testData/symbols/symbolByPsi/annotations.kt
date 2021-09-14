annotation class Anno(val param1: String, val param2: Int)

@Anno(param1 = "param", 2)
class X {
    @Anno("funparam", 3)
    fun x() {

    }
}
