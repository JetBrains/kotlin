class A(val value: String)

fun A.test(): String {
    val o = object  {
        val z: String
        init {
            val x = value + "K"
            z = x
        }
    }
    return o.z
}

fun box(): String {
    return A("O").test()
}