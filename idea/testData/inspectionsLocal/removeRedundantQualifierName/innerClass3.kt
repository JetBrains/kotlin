package my.simple.name

class Outer {
    class Middle {
        class Inner {
            fun otherCheck() {
                Outer.Middle.Inner<caret>.Companion.check()
            }

            companion object {
                fun check() {}
            }
        }
    }
}
