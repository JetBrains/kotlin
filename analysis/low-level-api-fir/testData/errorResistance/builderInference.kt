// WITH_STDLIB

import broken.lib.Foo

fun test() {
    buildList {
        add(1)
        add(Foo("foo", 1).result)
    }
}

fun <T> consume(a: T) {}