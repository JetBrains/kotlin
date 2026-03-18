// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-14059

// KT-14059: @Deprecated(level = HIDDEN) nested class still visible in subclass scope
open class A {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    class B
}

class B
class C : A() {
    fun test(b: B) { // should resolve to top-level B, not A.B
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, stringLiteral */
