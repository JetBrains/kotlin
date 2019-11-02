interface Base {
    open  fun test() : String  {
        return "base fail"
    }

}
interface Base2, Base {
    override fun test() : String  {
        return "base 2fail"
    }

}
class Delegate: Base {
    override fun test() : String  {
        return "OK"
    }

}
fun box() : String  {
    return object: Base2, Base {
}.test()
}
