class Pair {
    operator fun component1(): Int = null!!
    operator fun component2(): Int = null!!
}

class Coll {
    operator fun iterator(): It = It()
}

class It {
    operator fun next() = Pair()
    operator fun hasNext() = false
}


fun f() {
    for (val i in 1..4) {

    }

    for (var i in 1..4) {

    }

    for (val (i,j) in Coll()) {

    }

    for (var (i,j) in Coll()) {

    }
}
