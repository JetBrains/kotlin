// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE
class C(val x: Int)

<!CONFLICTING_OVERLOADS!>typealias CC = C<!>

<!CONFLICTING_OVERLOADS!>fun CC(x: Int)<!> = x

class Outer {
    class C(val x: Int)

    <!CONFLICTING_OVERLOADS!>typealias CC = C<!>

    <!CONFLICTING_OVERLOADS!>fun CC(x: Int)<!> = x
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, primaryConstructor, propertyDeclaration,
typeAliasDeclaration */
