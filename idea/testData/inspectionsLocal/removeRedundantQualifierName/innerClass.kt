package my.simple.name

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
    my.simple.name<caret>.Outer.Middle.Inner.check()
    Outer.Middle.Inner.check()
}