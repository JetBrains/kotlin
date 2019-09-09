// "Add '@JvmName' annotation" "true"
// WITH_RUNTIME
class Foo {
    fun <caret>bar(foo: List<String>): String {
        return "1"
    }

    fun bar(foo: List<Int>): String {
        return "2"
    }
}
