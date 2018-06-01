// ALLOW_AST_ACCESS
// SKIP_BINARY_TEST
class A {
    var foo = bar()

    @JvmField
    var foobaz = bar()
}

fun bar(): String = ""
