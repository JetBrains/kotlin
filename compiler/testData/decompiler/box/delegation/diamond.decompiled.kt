interface Base {
    abstract  fun getValue() : String
    open  fun test() : String  {
        return this.getValue()
    }

}
interface Base2, Base {
    override fun test() : String  {
        return "O" + this.getValue()
    }

}
interface KBase, Base {
}
interface Derived, KBase, Base2 {
}
class Fail: Derived {
    override fun getValue() : String  {
        return "Fail"
    }

}
fun box() : String  {
    val z : <no name provided> = <no name provided>()
    return z.test()
}
