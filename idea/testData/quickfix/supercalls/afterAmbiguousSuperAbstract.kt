// "Specify an explicit supertype" "true"
trait T {
    fun foo()
}

open class C1 {
    fun foo() {

    }
}

class C2 : T, C1() {
    fun bar() {
        super<caret><C1>.foo();
    }
}