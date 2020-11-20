import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Boolean {
    contract {
        returns(false) implies (x != null)
    }

    return x == null
}

@OptIn(ExperimentalContracts::class)
fun test2(x: String?): Boolean {
    contract {
        returns(true) implies (x != null)
    }

    return x != null
}

@OptIn(ExperimentalContracts::class)
fun test3(x: String?): Any? {
    contract {
        returns(true) implies (x != null)
    }

    if(x != null){
        return true
    }

    return false
}

@OptIn(ExperimentalContracts::class)
fun test4(x: String?): Any? {
    contract {
        returns(true) implies (x != null)
    }

    val y = x

    if(y != null){
        return true
    }

    return false
}

@OptIn(ExperimentalContracts::class)
fun test5(x: String?): Any? {
    contract {
        returns(true) implies (x != null)
    }

    return test2(x)
}