import kotlin.jvm.*

<!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun foo()<!> {}

class C {
    <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun foo()<!> {}

    companion object {
        <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun foo()<!> {}
    }
}

object O {
    <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun foo()<!> {}
}

fun test() {
    class Local {
        <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun foo()<!> {}
    }

    object {
        <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun foo()<!> {}
    }
}