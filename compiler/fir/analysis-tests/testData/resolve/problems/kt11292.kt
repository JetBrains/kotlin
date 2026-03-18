// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-11292

// KT-11292: Call resolver exceptions - class implementing itself by delegation should give proper error, not crash
class B : B by foo {
}
