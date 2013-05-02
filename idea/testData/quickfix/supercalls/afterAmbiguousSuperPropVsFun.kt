// "Specify an explicit supertype" "true"
open class C1 {
    val foo : Int = 3
}

trait T {
    fun foo() {

    }
}

class C2 : C1(), T {
    fun bar() {
        super<caret><C1>.foo;
    }
}