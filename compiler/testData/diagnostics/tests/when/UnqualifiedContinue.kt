enum class Move {LEFT, RIGHT}

fun inControlStructureBody(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            Move.RIGHT -> continue
            else -> continue
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inControlStructureBody2(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            else -> continue
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inWhenCondition(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            continue<!UNREACHABLE_CODE!><!>-> <!UNREACHABLE_CODE!>{}<!>
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inWhenCondition2(arr: Array<Move>): Move {
    for (move in arr) {
        when {
            continue -> <!UNREACHABLE_CODE!>{}<!>
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inWhenSubject(arr: Array<Move>): Move {
    for (move in arr) {
        when (continue) {
                <!UNREACHABLE_CODE!>true -> {}<!>
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}
