import kotlin.jvm.*

abstract class C {
    <!EXTERNAL_DECLARATION_CANNOT_BE_INLINED!><!NOTHING_TO_INLINE!>inline<!> external fun foo()<!>
}

fun test() {
    abstract class Local {
        <!EXTERNAL_DECLARATION_CANNOT_BE_INLINED!><!NOTHING_TO_INLINE!>inline<!> external fun foo()<!>
    }
}