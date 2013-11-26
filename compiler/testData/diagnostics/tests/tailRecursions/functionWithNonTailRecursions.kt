tailRecursive fun badTails(x : Int) : Int {
    if (x > 0) {
        return 1 + <!NON_TAIL_RECURSIVE_CALL!>badTails<!>(x - 1)
    }
    else if (x == 10) {
        [suppress("NON_TAIL_RECURSIVE_CALL")]
        return 1 + badTails(x - 1)
    } else if (x == 50) {
        return badTails(x - 1)
    }
    return 0
}