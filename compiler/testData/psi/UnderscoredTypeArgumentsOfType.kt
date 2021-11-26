fun main() {
    val x = foo<Foo<_>>()
    val x = foo<Foo<_>, _>()
    val x = foo<Foo<_>, Int>()
    val x = foo<Foo<Int, _>>()
    val x = foo<Foo<Int, Foo<_>, Float>, Float>()

    val y: Foo<_> = 1
    val y: Foo<_, _> = 1
}

interface A : Foo<_>

typealias Foo<K> = Foo<_, K>
