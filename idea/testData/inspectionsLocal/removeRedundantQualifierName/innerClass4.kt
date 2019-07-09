package my.simple.name

fun <T> check() {}
class Outer {
    class Middle {
        class Inner {
            fun foo() {
                Middle.Inner.Companion.check()
                my.simple.name<caret>.check<Outer>()
            }
            companion object {
                fun check() {}
            }
        }
    }
}