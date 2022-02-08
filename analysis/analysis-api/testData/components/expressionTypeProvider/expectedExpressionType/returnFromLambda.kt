fun x(): Int {
    receiveLambda {
        return@receiveLambda <caret>fd
    }
    return 2
}

fun receiveLambda(x: () -> Int){}

