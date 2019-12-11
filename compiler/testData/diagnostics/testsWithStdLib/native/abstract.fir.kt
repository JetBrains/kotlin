import kotlin.jvm.*

abstract class C {
    abstract external fun foo()
}

fun test() {
    abstract class Local {
        abstract external fun foo()
    }
}