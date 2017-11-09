// JVM_TARGET: 1.8
//WITH_RUNTIME

fun box(): String {
    val list = listOf("O", "K")
    return list.fold("") {a, b -> a +b}
}

