// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocks -CompanionExtensions +ExplicitContextArguments
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
class C1 {
    <!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>companion<!> {}
}

class C2 {
    <!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>companion<!> {
        fun foo() {}
    }
}

class C3 {
    <!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>companion<!> {}
    companion {
        val bar = 1
    }
}

val x = object {
    <!ILLEGAL_COMPANION_BLOCK("anonymous object"), UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>companion<!> {
        fun foo() {}
    }
}

enum class E {
    Entry {
        <!ILLEGAL_COMPANION_BLOCK("enum entry"), UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>companion<!> {
            fun foo() {}
        }
    };

    <!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>companion<!> {
        context(s: String)
        val entries get() = listOf(E.Entry)

        context(s: String)
        fun values() = arrayOf(E.Entry)

        context(s: String)
        fun valueOf(x: String) = E.Entry
    }
}

fun test() {
    C2.<!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>foo<!>()
    C3.<!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>bar<!>

    E.Entry

    E.entries
    E.values()
    E.valueOf("Entry")

    E.<!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>values<!>(s = "")
    E.<!UNSUPPORTED_FEATURE("The feature \"companion blocks\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks', but note that no stability guarantees are provided.")!>valueOf<!>("Entry", s = "")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
