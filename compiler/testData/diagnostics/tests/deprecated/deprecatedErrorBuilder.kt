// !DIAGNOSTICS: -UNUSED_PARAMETER
class Table
class Tr

fun table(body: Table.() -> Unit) {}
fun Table.tr(body: Tr.() -> Unit) {}
@Deprecated("Don't call me", level = DeprecationLevel.ERROR)
fun Tr.tr(body: Tr.() -> Unit) {}

fun builderTest() {
    table {
        tr {
            <!DEPRECATION_ERROR!>tr<!> {}
            table {
                tr {
                    <!DEPRECATION_ERROR!>tr<!> {}
                }
            }
        }
    }
}