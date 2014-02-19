public class Foo {
    fun get(i1: Int, i2: Int) : Int {
        return i1 + i2
    }
}

fun bar(){
    val f = Foo()
    f.get<caret>(1,2)
}