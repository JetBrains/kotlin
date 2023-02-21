enum class Move {LEFT, RIGHT}

fun inControlStructureBody(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            Move.RIGHT -> break
            else -> break
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inControlStructureBody2(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            else -> break
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inWhenCondition(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            break-> {}
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inWhenCondition2(arr: Array<Move>): Move {
    for (move in arr) {
        when {
            break -> {}
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inWhenSubject(arr: Array<Move>): Move {
    for (move in arr) {
        when (break) {
                true -> {}
        }
        return Move.LEFT
    }
    return Move.RIGHT
}