// PROBLEM: none
class Some {
    operator fun invoke() {}
}

class Other {
    fun foo() {
        val a = Some()
        <caret>this.a()
        a()
    }

    fun a() {}
}