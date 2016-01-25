// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.JvmStatic

abstract class A {

    open fun a() {}

    abstract fun b()

    open fun c() {}
}

object B: A() {

    <!OVERRIDE_CANNOT_BE_STATIC!>@JvmStatic override fun a()<!> {}

    <!OVERRIDE_CANNOT_BE_STATIC!>@JvmStatic override fun b()<!> {}

    <!OVERRIDE_CANNOT_BE_STATIC!>@JvmStatic final override fun c()<!> {}

    @JvmStatic <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> fun d() {}
}

class C {

    companion object: A() {
        @JvmStatic override fun a() {}

        @JvmStatic override fun b() {}

        @JvmStatic final override fun c() {}

        @JvmStatic <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> fun d() {}
    }
}