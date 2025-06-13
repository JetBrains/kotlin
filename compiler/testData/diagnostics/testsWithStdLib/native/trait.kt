// RUN_PIPELINE_TILL: FRONTEND
import kotlin.jvm.*

interface Tr {
    <!EXTERNAL_DECLARATION_IN_INTERFACE!>external fun foo()<!>
    <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY, EXTERNAL_DECLARATION_IN_INTERFACE!>external fun bar()<!> {}

    companion object {
        external fun foo()
        <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun bar()<!> {}
    }
}

/* GENERATED_FIR_TAGS: companionObject, external, functionDeclaration, interfaceDeclaration, objectDeclaration */
