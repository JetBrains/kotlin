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

@Y
external class B {
    @Y
    fun f()

    @Y
    val p: Int

    @get:Y
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
