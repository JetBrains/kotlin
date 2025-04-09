// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOverloadClashesByErasure
// ISSUE: KT-13712

interface B<T> {
    fun T.foo() {}
}

open class A {
    fun Any.foo() {}
}

class C : B<String>, A() {
    override fun String.<!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_WARNING!>foo<!>() {}
}
