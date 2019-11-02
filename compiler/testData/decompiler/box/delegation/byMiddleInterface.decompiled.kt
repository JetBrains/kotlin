interface Base {
    abstract  fun getValue() : String
    open  fun test() : String  {
        return this.getValue()
    }

}
interface BaseKotlin, Base {
}
class Fail: BaseKotlin {
    override fun getValue() : String  {
        return "Fail"
    }

}
fun box() : String  {
    val z : <no name provided> = <no name provided>()
    return z.test()
}
