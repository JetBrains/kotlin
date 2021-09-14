fun /* EXPECTED_TARGET */x(): Int {
    receiveLambda {
        return<caret> 1
    }
    return 2
}

inline fun receiveLambda(x: () -> Unit){}