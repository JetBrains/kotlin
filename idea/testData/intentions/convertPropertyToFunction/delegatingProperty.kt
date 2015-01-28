// WITH_RUNTIME
// IS_APPLICABLE: false
import kotlin.properties.Delegates

class A(val n: Int) {
    val <caret>foo: Boolean by Delegates.lazy { n > 1 }
}

fun test() {
    val t = A(1).foo
}