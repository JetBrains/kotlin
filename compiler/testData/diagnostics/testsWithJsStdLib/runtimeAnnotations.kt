// FIR_IDENTICAL
// !DIAGNOSTICS: +RUNTIME_ANNOTATION_NOT_SUPPORTED
@Retention(AnnotationRetention.BINARY)
annotation class X

@Retention(AnnotationRetention.RUNTIME)
annotation class Y

@X
external class A {
    @X
    fun f()

    @X
    val p: Int

    @get:X
    val r: Int
}

<!RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION!>@Y<!>
external class B {
    <!RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION!>@Y<!>
    fun f()

    <!RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION!>@Y<!>
    val p: Int

    <!RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION!>@get:Y<!>
    val r: Int
}

@X
class C {
    @X
    fun f() {}

    @X
    val p: Int = 0

    val q: Int
        @X get() = 0

    @get:X
    val r: Int = 0
}

<!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@Y<!>
class D {
    <!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@Y<!>
    fun f() {}

    <!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@Y<!>
    val p: Int = 0

    val q: Int
      <!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@Y<!> get() = 0

    <!RUNTIME_ANNOTATION_NOT_SUPPORTED!>@get:Y<!>
    val r: Int = 0
}