// IS_APPLICABLE: false
public class Foo {
    fun get() : Int {
        return 0
    }
}

fun bar(){
    val f = Foo()
    f.get<caret>()
}