import kotlin.jvm.*

interface Tr {
    <!EXTERNAL_DECLARATION_IN_INTERFACE!>external fun foo()<!>
    <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY, EXTERNAL_DECLARATION_IN_INTERFACE!>external fun bar()<!> {}

    companion object {
        external fun foo()
        <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun bar()<!> {}
    }
}