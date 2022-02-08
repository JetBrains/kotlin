// !LANGUAGE: +ContextReceivers

class Context

<!CONFLICTING_OVERLOADS!>context(Context)
fun f(): String<!> = TODO()

<!CONFLICTING_OVERLOADS!>fun f(): Any<!> = TODO()

fun test() {
    with(Context()) {
        f().length
    }
}