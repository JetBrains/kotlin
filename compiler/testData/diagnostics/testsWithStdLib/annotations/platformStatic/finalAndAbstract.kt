// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.jvmStatic

abstract class A {

    open fun a() {}

    abstract fun b()

    open fun c() {}
}

object B: A() {

    <!OVERRIDE_CANNOT_BE_STATIC!>@jvmStatic override fun a()<!> {}

    <!OVERRIDE_CANNOT_BE_STATIC!>@jvmStatic override fun b()<!> {}

    <!OVERRIDE_CANNOT_BE_STATIC!>@jvmStatic final override fun c()<!> {}

    @jvmStatic open fun d() {}
}

class C {

    companion object: A() {
        @jvmStatic override fun a() {}

        @jvmStatic override fun b() {}

        @jvmStatic final override fun c() {}

        @jvmStatic open fun d() {}
    }
}