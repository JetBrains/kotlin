// WITH_RUNTIME
package my.simple.name

typealias Int = Long

class Outer {
    class Middle {
        class Int
        class Inner {
            fun goo(i: Outer.Middle<caret>.Int) {

            }
        }
    }
}
