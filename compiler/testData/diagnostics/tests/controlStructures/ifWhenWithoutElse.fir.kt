fun idAny(x: Any) = x
fun <T> id(x: T) = x
fun idUnit(x: Unit) = x

class MList {
    // MutableCollection<T>.add returns Boolean, but nobody cares
    fun add(): Boolean = true
}
val mlist = MList()

fun work() {}

val xx1 = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42
val xx2: Unit = <!INITIALIZER_TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42<!>
val xx3 = idAny(<!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42)
val xx4 = id(<!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42)
val xx5 = idUnit(<!ARGUMENT_TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42<!>)
val xx6 = null ?: <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42
val xx7 = "" + <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42

val wxx1 = <!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }
val wxx2: Unit = <!INITIALIZER_TYPE_MISMATCH!><!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }<!>
val wxx3 = idAny(<!NO_ELSE_IN_WHEN!>when<!> { true -> 42 })
val wxx4 = id(<!NO_ELSE_IN_WHEN!>when<!> { true -> 42 })
val wxx5 = idUnit(<!ARGUMENT_TYPE_MISMATCH!><!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }<!>)
val wxx6 = null ?: <!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }
val wxx7 = "" + <!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }

val fn1 = { if (true) 42 }
val fn2 = { if (true) mlist.add() }
val fn3 = { if (true) work() }
val fn4 = { when { true -> 42 } }
val fn5 = { when { true -> mlist.add() } }
val fn6 = { when { true -> work() } }

val ufn1: () -> Unit = { if (true) 42 }
val ufn2: () -> Unit = { if (true) mlist.add() }
val ufn3: () -> Unit = { if (true) work() }
val ufn4: () -> Unit = { when { true -> 42 } }
val ufn5: () -> Unit = { when { true -> mlist.add() } }
val ufn6: () -> Unit = { when { true -> work() } }

fun f1() = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) work()
fun f2() = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) mlist.add()
fun f3() = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42
fun f4(): Unit = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) work()
fun f5(): Unit = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) mlist.add()
fun f6(): Unit = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42
fun g1() = <!NO_ELSE_IN_WHEN!>when<!> { true -> work() }
fun g2() = <!NO_ELSE_IN_WHEN!>when<!> { true -> mlist.add() }
fun g3() = <!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }
fun g4(): Unit = <!NO_ELSE_IN_WHEN!>when<!> { true -> work() }
fun g5(): Unit = <!NO_ELSE_IN_WHEN!>when<!> { true -> mlist.add() }
fun g6(): Unit = <!NO_ELSE_IN_WHEN!>when<!> { true -> 42 }

fun foo1(x: String?) {
    "" + <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 42
    w@while (true) {
        x ?: <!INVALID_IF_AS_EXPRESSION!>if<!> (true) break
        x <!USELESS_ELVIS!>?: <!NO_ELSE_IN_WHEN!>when<!> { true -> break@w }<!>
    }
}

fun foo2() {
    if (true) {
        mlist.add()
    }
    else if (true) {
        mlist.add()
    }
    else if (true) {
        mlist.add()
    }

    when {
        true -> mlist.add()
        else -> when {
            true -> mlist.add()
            else -> when {
                true -> mlist.add()
            }
        }
    }
}

