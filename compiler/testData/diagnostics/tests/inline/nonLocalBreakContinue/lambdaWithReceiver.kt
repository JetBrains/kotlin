// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// ISSUE: KT-1436

public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}

fun test() {
    val list = listOf(User("Masha"), User("Kate"))
    for (i in list) with(i) {
        if (name == "Kate") break
        if (name == "Masha") continue
    }
}

class User(val name: String)