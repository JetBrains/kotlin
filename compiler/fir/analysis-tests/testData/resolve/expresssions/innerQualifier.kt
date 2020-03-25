class Outer {
    inner class Inner
}

val x = Outer.Inner
val klass = Outer.Inner::class