fun x(): Int {
    receiveLambda { /* EXPECTED_TARGET */
        return@receiveLambda<caret> 1
    }
    return 2
}

fun receiveLambda(x: () -> Int){}