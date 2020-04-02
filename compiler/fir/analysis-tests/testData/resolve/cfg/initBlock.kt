// !DUMP_CFG
class Foo {
    init {
        val x = 1
    }
}

class Bar {
    init {
        val x = 1
        throw Exception()
        val y = 2
    }
}