trait A : MyInt {
    override public fun test(): String? {
        return "OK"
    }
}

class B: A

fun box() : String {
    return B().test()!!
}