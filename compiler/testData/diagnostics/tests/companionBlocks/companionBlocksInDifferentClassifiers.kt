// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// RENDER_DIAGNOSTIC_ARGUMENTS

object O {
    <!ILLEGAL_COMPANION_BLOCK("object")!>companion<!> {
        fun foo() {}
    }
}

class C {
    companion object {
        <!ILLEGAL_COMPANION_BLOCK("object")!>companion<!> {
            fun foo() {}
        }
    }

    class Nested {
        companion {
            fun foo() {}
        }
    }
}

interface I {
    companion {
        fun foo() {}
    }
}

enum class E {
    Entry {
        fun test() {
            bar()
        }

        <!ILLEGAL_COMPANION_BLOCK("enum entry")!>companion<!> {
            fun bar() {}
        }
    };

    companion {
        fun foo() {
            Entry
        }
    }
}

data class D(val x: String) {
    companion {
        fun foo() {}
    }
}

annotation class Ann(val x: String) {
    companion {
        <!ANNOTATION_CLASS_MEMBER!>fun foo()<!> {}
    }
}

val x = object {
    <!ILLEGAL_COMPANION_BLOCK("anonymous object")!>companion<!> {
        fun foo() {}
    }

    fun test() {
        foo()
    }
}

fun test() {
    C.Nested.foo()
    I.foo()
    E.foo()
    D.foo()
    Ann.foo()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, enumDeclaration, enumEntry, functionDeclaration,
interfaceDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration */
