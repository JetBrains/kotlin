// IS_APPLICABLE: false

class Owner(val z: Int) {
    // Possible only in 1.1 with bound references (this::foo)
    val x = { arg: Int <caret> -> foo(arg) }
}

fun Owner.foo(y: Int) = y + z

