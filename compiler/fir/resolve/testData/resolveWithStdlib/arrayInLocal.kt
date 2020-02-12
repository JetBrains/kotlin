fun foo() {
    fun convert(vararg paths: String): Array<String> = paths.toList().toTypedArray()

    convert("1", "2", "3")
}