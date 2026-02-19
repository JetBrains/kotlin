import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun myRequire(b: Boolean) {
    contract {
        returns() implies (b)
    }
    if (!b) throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
fun myRequireAnd(b1: Boolean, b2: Boolean) {
    contract {
        returns() implies (b1 && b2)
    }
    if (!(b1 && b2)) throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
fun myRequireOr(b1: Boolean, b2: Boolean) {
    contract {
        returns() implies (b1 || b2)
    }
    if (!(b1 || b2)) throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
fun myRequireNot(b: Boolean) {
    contract {
        returns() implies (!b)
    }
    if (b) throw IllegalStateException()
}
