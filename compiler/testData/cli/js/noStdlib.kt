// See KT-69165

fun foo() {
    String::class
    Void::class
    ::foo.name
    listOf(42)
}
