interface T {
    @kotlin.jvm.JvmOverloads fun foo(s: String = "OK")

    @kotlin.jvm.JvmOverloads fun bar(s: String = "OK") {
    }
}


abstract class C {
    @kotlin.jvm.JvmOverloads abstract fun foo(s: String = "OK")
}