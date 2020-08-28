import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun wrongFalse(x: String?): Boolean {
    contract {
        returns(false) implies (x != null)
    }

    return true
}

@OptIn(ExperimentalContracts::class)
fun wrongTrue(x: String?): Boolean {
    contract {
        returns(true) implies (x != null)
    }

    return false
}

@OptIn(ExperimentalContracts::class)
fun nullableConstant(x: String?): Any? {
    contract {
        returns(null) implies (x != null)
    }

    return 1
}

fun string() : String = ""

@OptIn(ExperimentalContracts::class)
fun nullableReturn(x: String?): Any? {
    contract {
        returns(null) implies (x != null)
    }

    return string()
}

@OptIn(ExperimentalContracts::class)
fun returnsNull(x: String?): Any? {
    contract {
        returnsNotNull() implies (x != null)
    }

    return null
}

@OptIn(ExperimentalContracts::class)
fun wrongReturnType(x: String?): Any? {
    contract {
        returns(true) implies (x != null)
    }

    return "true"
}