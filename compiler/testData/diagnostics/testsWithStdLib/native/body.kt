import kotlin.jvm.*

<!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun foo()<!> {}

class C {
    <!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun foo()<!> {}

    default object {
        <!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun foo()<!> {}
    }
}

object O {
    <!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun foo()<!> {}
}

fun test() {
    class Local {
        <!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun foo()<!> {}
    }

    object {
        <!NATIVE_DECLARATION_CANNOT_HAVE_BODY!>native fun foo()<!> {}
    }
}