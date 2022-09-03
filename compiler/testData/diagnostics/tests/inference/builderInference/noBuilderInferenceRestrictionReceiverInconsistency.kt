// FIR_IDENTICAL
// SKIP_TXT
// WITH_STDLIB
// LANGUAGE: +NoBuilderInferenceWithoutAnnotationRestriction

class A
class B

var B.foo: Boolean
    get() = true
    set(value) {}

private fun A.bar(b: B) {
    b.foo = true
}