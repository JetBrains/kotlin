// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.platform.platformStatic

abstract class A {

    open fun a() {}

    abstract fun b()

    open fun c() {}
}

object B: A() {

    <!OVERRIDE_CANNOT_BE_STATIC!>[platformStatic] override fun a()<!> {}

    <!OVERRIDE_CANNOT_BE_STATIC!>[platformStatic] override fun b()<!> {}

    <!OVERRIDE_CANNOT_BE_STATIC!>[platformStatic] final override fun c()<!> {}

    [platformStatic] open fun d() {}
}

class C {

    default object: A() {
        [platformStatic] override fun a() {}

        [platformStatic] override fun b() {}

        [platformStatic] final override fun c() {}

        [platformStatic] open fun d() {}
    }
}