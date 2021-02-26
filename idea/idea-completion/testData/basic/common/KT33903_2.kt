// FIR_COMPARISON
import Obj.foo
import Obj.foo

object Obj {
    fun Any.foo() {}
}

fun usage() {
    Any().foo<caret>
}

// EXIST: foo
// NUMBER: 1
