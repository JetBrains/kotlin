import kotlin.jvm.*

abstract class C {
    <!NATIVE_DECLARATION_CANNOT_BE_ABSTRACT!>abstract<!> native fun foo()
}

fun test() {
    abstract class Local {
        <!NATIVE_DECLARATION_CANNOT_BE_ABSTRACT!>abstract<!> native fun foo()
    }
}