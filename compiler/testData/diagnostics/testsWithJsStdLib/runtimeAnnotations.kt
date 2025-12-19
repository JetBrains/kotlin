// RUN_PIPELINE_TILL: FRONTEND
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

typealias TY = Y

<!RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION!>@TY<!>
external class BB

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

@Y
class D {
    @Y
    fun f() {}

    @Y
    val p: Int = 0

    val q: Int
      @Y get() = 0

    @get:Y
    val r: Int = 0
}