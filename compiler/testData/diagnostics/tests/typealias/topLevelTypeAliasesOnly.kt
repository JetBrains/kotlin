// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +LocalTypeAliases
// RENDER_DIAGNOSTICS_FULL_TEXT

typealias TopLevel = Any

interface A {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Nested = Any<!>
}

class C {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Nested = Any<!>
    class D {
        <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Nested = Any<!>
        fun foo() {
            <!TOPLEVEL_TYPEALIASES_ONLY!>typealias LocalInMember = Any<!>
        }
    }
}

fun foo() {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Local = Any<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nestedClass, typeAliasDeclaration */
