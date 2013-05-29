// "Specify an explicit supertype" "true"
open class C1 {
    fun foo(a: Int) {

    }
}

trait T {
    fun foo(b: String) {

    }
}

class C2 : C1(), T {
    public fun bar() {
        val c : Int = 0;
        super<caret>.foo(c);
    }
}