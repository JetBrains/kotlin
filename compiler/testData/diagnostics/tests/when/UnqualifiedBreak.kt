enum class Move {LEFT, RIGHT}

fun inControlStructureBody(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            Move.RIGHT -> break
            else -> break
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inControlStructureBody2(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            else -> break
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inWhenCondition(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            break<!UNREACHABLE_CODE!><!>-> <!UNREACHABLE_CODE!>{}<!>
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inWhenCondition2(arr: Array<Move>): Move {
    for (move in arr) {
        when {
            break -> <!UNREACHABLE_CODE!>{}<!>
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}

fun inWhenSubject(arr: Array<Move>): Move {
    for (move in arr) {
        when (break) {
                <!UNREACHABLE_CODE!>true -> {}<!>
        }
        <!UNREACHABLE_CODE!>return Move.LEFT<!>
    }
    return Move.RIGHT
}
