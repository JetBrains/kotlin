// RUN_PIPELINE_TILL: BACKEND
// IGNORE_DEXING
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    open fun foo() {}
}

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!> : A, <!SUPERTYPE_APPEARS_TWICE{JVM}!>B<!> {}

// MODULE: jvm()()(common)

actual typealias B = A
