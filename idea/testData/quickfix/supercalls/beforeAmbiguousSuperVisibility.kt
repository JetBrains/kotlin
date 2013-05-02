// "Specify an explicit supertype" "true"
open class C1 {
    private fun foo() {

    }
}

trait T {
    public fun foo() {

    }
}

class C2 : C1(), T {
    public fun bar() {
        super<caret>.foo();
    }
}