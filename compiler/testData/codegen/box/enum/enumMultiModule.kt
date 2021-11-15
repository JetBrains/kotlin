// WITH_STDLIB
// MODULE: lib
// FILE: common.kt

enum class FooEnum(val s: String) {
    O("O"),
    FAIL("FAIL"),
    K("K");
}


// MODULE: bar(lib)
// FILE: second.kt

fun bar(): String = FooEnum.valueOf("O").s + FooEnum.values()[2].s

// MODULE: main(bar)
// FILE: main.kt

fun box(): String {
    return bar()
}