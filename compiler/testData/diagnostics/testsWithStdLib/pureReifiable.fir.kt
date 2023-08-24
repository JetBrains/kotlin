// !DIAGNOSTICS: -UNUSED_PARAMETER

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
inline fun <reified @kotlin.internal.PureReifiable T> foo(x: T) {}

fun test() {
    foo<List<String>>(listOf(""))
    foo(listOf(""))

    foo<Array<String>>(arrayOf(""))
    foo(arrayOf(""))
}
