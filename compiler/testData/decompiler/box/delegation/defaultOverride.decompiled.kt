interface Base {
    abstract  fun getValue() : String
    open  fun test() : String  {
        return this.getValue()
    }

}
interface BaseKotlin, Base {
    override fun getValue() : String  {
        return "OK"
    }

    override fun test() : String  {
        return this.getValue()
    }

}
class OK: BaseKotlin {
    override fun getValue() : String  {
        return "OK"
    }

}
fun box() : String  {
    val ok : <no name provided> = <no name provided>()
    return ok.test()
}
