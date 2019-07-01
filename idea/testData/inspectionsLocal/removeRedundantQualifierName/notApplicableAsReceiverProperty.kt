// PROBLEM: none

package inspector.p30879
import inspector.p30879.B.G

val <T> T.letVar: Int; get() = 0

fun test() {
    C<caret>.G.letVar
}

class B { object G }
class C { object G }
