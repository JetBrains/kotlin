package my.simple.name

import my.simple.name.Outer.Middle.Inner.Companion.check

class Outer {
    class Middle {
        class Inner {
            companion object {
                fun check() {}
            }
        }
    }
}

fun main() {
    my.simple.name.Outer.Middle.Inner.check()
    Outer.Middle.Inner<caret>.check()
}