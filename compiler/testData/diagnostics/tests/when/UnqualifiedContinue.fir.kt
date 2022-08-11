enum class Move {LEFT, RIGHT}

fun inControlStructureBody(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            Move.RIGHT -> continue
            else -> continue
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inControlStructureBody2(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            else -> continue
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inWhenCondition(arr: Array<Move>): Move {
    for (move in arr) {
        when (move) {
            continue-> {}
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inWhenCondition2(arr: Array<Move>): Move {
    for (move in arr) {
        when {
            continue -> {}
        }
        return Move.LEFT
    }
    return Move.RIGHT
}

fun inWhenSubject(arr: Array<Move>): Move {
    for (move in arr) {
        when (continue) {
                true -> {}
        }
        return Move.LEFT
    }
    return Move.RIGHT
}