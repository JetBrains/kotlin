fun foo2(p: () -> Int): Int = p()

fun foo3(p: () -> Int) {}

fun bar() {
    foo3 {
        foo2 {
            <expr>if (flag()) {
                return@foo3 2
            }
            1</expr>
        }
        2
    }
}

fun flag() = true