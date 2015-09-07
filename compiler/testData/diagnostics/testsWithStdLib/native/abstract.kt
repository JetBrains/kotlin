import kotlin.jvm.*

abstract class C {
    <!EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT!>abstract<!> external fun foo()
}

fun test() {
    abstract class Local {
        <!EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT!>abstract<!> external fun foo()
    }
}