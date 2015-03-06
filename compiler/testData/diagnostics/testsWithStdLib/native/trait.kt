import kotlin.jvm.*

trait Tr {
    <!NATIVE_DECLARATION_IN_TRAIT!>native fun foo()<!>
    <!NATIVE_DECLARATION_IN_TRAIT, NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun bar()<!> {}

    default object {
        native fun foo()
        <!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun bar()<!> {}
    }
}