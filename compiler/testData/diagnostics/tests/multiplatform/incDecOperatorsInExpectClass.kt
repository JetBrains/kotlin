// RUN_PIPELINE_TILL: BACKEND
// Issue: KT-49714

// MODULE: common
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Counter<!> {
    operator fun inc(): Counter
    operator fun dec(): Counter
}

// MODULE: main()()(common)
actual typealias Counter = Int
