class Klass {
    fun unusedFun() {
    }

    @Suppress("UnusedSymbol")
    fun unusedNoWarn() {

    }
}

@Suppress("UnusedSymbol")
class OtherKlass {
    fun unusedNoWarn() {

    }
}

fun main(args: Array<String>) {
    Klass()
    OtherKlass()
}