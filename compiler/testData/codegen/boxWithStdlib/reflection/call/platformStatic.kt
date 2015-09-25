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
    (Obj::foo).call(Obj)
    (C.Companion::bar).call(C.Companion)
    return "OK"
}
