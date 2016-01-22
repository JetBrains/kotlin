// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

enum class Foo {
    FOO;

    companion object {
        fun valueOf(something: String) = 2

        fun values() = 1
    }
}

fun test() {
    Foo.values() checkType { _<Array<Foo>>() }
    Foo.Companion.values() checkType { _<Int>() }

    Foo.valueOf("") checkType { _<Foo>() }
    Foo.Companion.valueOf("") checkType { _<Int>() }
}