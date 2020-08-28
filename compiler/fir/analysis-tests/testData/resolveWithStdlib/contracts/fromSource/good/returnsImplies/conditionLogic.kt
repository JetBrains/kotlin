import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Any? {
    contract {
        returnsNotNull() implies (!(x == null))
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun test2(x: String?): Any? {
    contract {
        returnsNotNull() implies (x is String && x != null)
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun test3(x: String?): Any? {
    contract {
        returnsNotNull() implies (x is String? || x is Any?)
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun test4(x: String?, y: String?): Any? {
    contract {
        returns(true) implies (x != null && y != null)
    }

    return x != null && y != null
}

@OptIn(ExperimentalContracts::class)
fun test5(x: Any?): Any? {
    contract {
        returns(true) implies (x != null || x is Any)
    }

    return x != null
}

@OptIn(ExperimentalContracts::class)
fun test6(x: Any?): Any? {
    contract {
        returns(true) implies (x is String? && x != null)
    }

    return x is String
}

@OptIn(ExperimentalContracts::class)
fun test7(x: Any?): Any? {
    contract {
        returns(true) implies (x is String? && x != null || x is Int)
    }

    return x is String
}

@OptIn(ExperimentalContracts::class)
fun test8(x: Any?): Any? {
    contract {
        returns(true) implies (x is String || x is Int)
    }

    return x is String || x is Int
}

@OptIn(ExperimentalContracts::class)
fun test9(x: Any?): Any? {
    contract {
        returns(true) implies (x is String || x is Int)
    }

    if(x is String){
        return true
    }
    return x is Int
}

@OptIn(ExperimentalContracts::class)
fun test10(x: Any?): Any? {
    contract {
        returns(true) implies (x is Comparable<*> || x is CharSequence)
    }

    return x is String
}

@OptIn(ExperimentalContracts::class)
fun test11(x: Any?): Any? {
    contract {
        returns(true) implies (x is Comparable<*> && x is CharSequence)
    }

    return x is String
}

@OptIn(ExperimentalContracts::class)
fun test12(x: Any?): Any? {
    contract {
        returns(true) implies (x is Comparable<*> && (x is CharSequence || x is Number))
    }

    return x is String || x is Int
}

@OptIn(ExperimentalContracts::class)
fun test13(x: Any?): Any? {
    contract {
        returns(true) implies (!(x !is Comparable<*> || (x !is CharSequence && !(x is Number))))
    }

    return x is String || x is Int
}