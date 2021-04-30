class Outer {
    inner class Inner
}

val x = Outer.<!NO_COMPANION_OBJECT!>Inner<!>
val klass = Outer.Inner::class
