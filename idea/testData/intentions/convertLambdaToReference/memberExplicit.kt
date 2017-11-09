// IS_APPLICABLE: true

class Owner(val z: Int) {
    fun foo(y: Int) = y + z
}

val owner = Owner(42)

val x = { arg: Int <caret> -> owner.foo(arg) }
