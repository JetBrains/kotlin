// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-76773
// WITH_STDLIB

class Klass

context(_: Klass) fun foo() {
    contextOf()
}