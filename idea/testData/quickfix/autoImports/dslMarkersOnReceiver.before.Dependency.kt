
@DslMarker
annotation class DSL

@DSL
class AAA {
    fun sub(l: @DSL BBB.() -> Unit) {
        l(BBB())
    }
}

class BBB

object AExtSpace {
    fun AAA.aaa() {

    }
}

object BExtSpace {
    fun BBB.aaa() {

    }
}
