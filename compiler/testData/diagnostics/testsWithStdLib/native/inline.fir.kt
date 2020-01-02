import kotlin.jvm.*

abstract class C {
    inline external fun foo()
}

fun test() {
    abstract class Local {
        inline external fun foo()
    }
}