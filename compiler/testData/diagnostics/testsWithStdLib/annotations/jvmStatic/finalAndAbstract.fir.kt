// !DIAGNOSTICS: -UNUSED_VARIABLE
abstract class A {

    open fun a() {}

    abstract fun b()

    open fun c() {}
}

object B: A() {

    @JvmStatic override fun a() {}

    @JvmStatic override fun b() {}

    @JvmStatic final override fun c() {}

    @JvmStatic open fun d() {}
}

class C {

    companion object: A() {
        @JvmStatic override fun a() {}

        @JvmStatic override fun b() {}

        @JvmStatic final override fun c() {}

        @JvmStatic open fun d() {}
    }
}