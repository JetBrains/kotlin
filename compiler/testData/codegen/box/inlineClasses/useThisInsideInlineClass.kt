// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class UInt(val a: Int) {
    fun test() {
        takeNullable(this)
        takeAnyInside(this)

        takeAnyInside(this.a)
    }

    fun takeAnyInside(a: Any) {}
}

fun takeNullable(a: UInt?) {}

fun box(): String {
    val a = UInt(0)
    a.test()

    return "OK"
}