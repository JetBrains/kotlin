// KT-16291 Smart cast doesn't work when getting class of instance

class Foo(val s: String) {
    override fun equals(other: Any?): Boolean {
        return other != null && other::class == this::class && s == (other as Foo).s
    }
}

fun box(): String {
    return if (Foo("a") == Foo("a")) "OK" else "Fail"
}
