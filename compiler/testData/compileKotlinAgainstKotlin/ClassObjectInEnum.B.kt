import library.EnumClass

fun main(args: Array<String>) {
    if (EnumClass.entry() != EnumClass.ENTRY) throw AssertionError()
}
