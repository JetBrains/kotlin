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
    for (<!VAL_OR_VAR_ON_LOOP_PARAMETER!>val<!> i in 1..4) {

    }

    for (<!VAL_OR_VAR_ON_LOOP_PARAMETER!>var<!> i in 1..4) {

    }

    for (<!VAL_OR_VAR_ON_LOOP_PARAMETER!>val<!> (i,<!UNUSED_VARIABLE!>j<!>) in Coll()) {

    }

    for (<!VAL_OR_VAR_ON_LOOP_PARAMETER!>var<!> (i,<!UNUSED_VARIABLE!>j<!>) in Coll()) {

    }
}
