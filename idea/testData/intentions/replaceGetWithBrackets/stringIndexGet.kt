public class Foo {
    fun get(bar: String) : Boolean {
        return true
    }
}

fun bar(){
    val f = Foo()
    f.get<caret>("test")
}