import kotlin.jvm.*

abstract class C {
    <!NOTHING_TO_INLINE!>inline<!> external fun foo()
}

fun test() {
    abstract class Local {
        <!NOTHING_TO_INLINE!>inline<!> external fun foo()
    }
}
