// IS_APPLICABLE: false

open class Some {
    fun bar() {}
}

object Obj : Some()

fun foo(arg: Any?) {
    <caret>if (arg is Some) {
        arg.bar()
    }
    else {
        Obj.bar()
    }
}