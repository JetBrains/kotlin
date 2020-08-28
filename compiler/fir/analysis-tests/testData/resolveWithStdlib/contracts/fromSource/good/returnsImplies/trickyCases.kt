import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Any? {
    contract {
        returnsNotNull() implies (x != null)
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun test2(x: String?): Any? {
    contract {
        returns(true) implies (x != null)
    }

    return if(x != null) true else false
}

@OptIn(ExperimentalContracts::class)
fun test3(x: Any?): Any? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return if(true) x else null
}

@OptIn(ExperimentalContracts::class)
fun test4(x: Any?): Any? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return if(x != null) {
        if (true) x else false
    } else {
        null
    }
}