class C {
    fun foo(a: Int = 1, f: (String) -> String): String = f("$a")

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun bar(a: Int = 1, f: (String) -> String): String = f("$a")
}