// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

object Obj {
    @JvmStatic
    fun foo() {}
}

class C {
    companion object {
        @JvmStatic
        fun bar() {}
    }
}

fun box(): String {
    (Obj::class.members.single { it.name == "foo" }).call(Obj)
    (C.Companion::class.members.single { it.name == "bar" }).call(C.Companion)
    return "OK"
}
