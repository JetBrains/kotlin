// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// ISSUE: KT-1436
// RENDER_DIAGNOSTICS_FULL_TEXT

public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}

fun test() {
    val list = listOf(User("Masha"), User("Kate"))
    for (i in list) with(i) {
        if (name == "Kate") <!UNSUPPORTED_FEATURE!>break<!>
        if (name == "Masha") <!UNSUPPORTED_FEATURE!>continue<!>
    }
}

class User(val name: String)
