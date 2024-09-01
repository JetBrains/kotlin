import kotlin.jvm.*

abstract class C {
    <!NOTHING_TO_INLINE!>inline<!> <!EXTERNAL_DECLARATION_CANNOT_BE_INLINED!>external<!> fun foo()
}

fun test() {
    abstract class Local {
        <!NOTHING_TO_INLINE!>inline<!> <!EXTERNAL_DECLARATION_CANNOT_BE_INLINED!>external<!> fun foo()
    }
}
