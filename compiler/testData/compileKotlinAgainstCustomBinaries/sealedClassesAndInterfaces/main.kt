import test.*

fun test_1(base: IBase) {
    val x = when (base) {
        is IA -> 1
        is B -> 2
        is C -> 3
        is D -> 4
    }
}

fun test_2(base: IBase) {
    val x = when (base) {
        is IA -> 1
        is B.First -> 2
        is B.Second -> 3
        C.SomeValue -> 4
        C.AnotherValue -> 5
        D -> 6
    }
}

fun test_3(base: Base) {
    val x = when (base) {
        is B -> 2
        is D -> 4
    }
}

fun test_4(base: Base) {
    val x = when (base) {
        is B.First -> 2
        is B.Second -> 3
        D -> 6
    }
}
