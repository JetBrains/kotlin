import kotlin.jvm.*

abstract class C {
    <!NATIVE_DECLARATION_CANNOT_BE_INLINED, NOTHING_TO_INLINE!>inline native fun foo()<!>
}

fun test() {
    abstract class Local {
        <!NATIVE_DECLARATION_CANNOT_BE_INLINED, NOTHING_TO_INLINE!>inline native fun foo()<!>
    }
}