import kotlin.jvm.JvmStatic as static

object Obj {
    @static fun foo() {}
}

class C {
    companion object {
        @static fun bar() {}
    }
}

fun box(): String {
    (Obj::class.members.single { it.name == "foo" }).call(Obj)
    (C.Companion::class.members.single { it.name == "bar" }).call(C.Companion)
    return "OK"
}
