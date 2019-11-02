interface Base {
    abstract  fun getValue() : String
    open  fun test() : String  {
        return this.getValue()
    }

}
class Fail: Base {
    override fun getValue() : String  {
        return "Fail"
    }

}
fun box() : String  {
    val z : <no name provided> = <no name provided>()
    return z.test()
}
