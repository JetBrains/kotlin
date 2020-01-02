package f


//KT-3444 Front-end doesn't check if local function or function of anonymous class returns value

fun box(): Int {

    fun local(): Int {
    }

    return local()
}

fun main() {
    box()
}

