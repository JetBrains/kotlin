fun String.k(): () -> String = { -> this }

fun test() = "hello".k()()