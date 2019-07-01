package my.simple.name

fun <T> check() {}
class Outer {
    class Middle {
        class Inner {
            fun foo() {
                Middle.Inner<caret>.Companion.check()
                my.simple.name.check<Outer>()
            }
            companion object {
                fun check() {}
            }
        }
    }
}