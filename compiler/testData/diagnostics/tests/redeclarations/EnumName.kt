// RUN_PIPELINE_TILL: FRONTEND
enum class A {
    <!REDECLARATION!>name<!>,
    <!REDECLARATION!>ordinal<!>,
    <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries,<!>
}
