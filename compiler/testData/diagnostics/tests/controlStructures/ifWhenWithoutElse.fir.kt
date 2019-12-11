// !WITH_NEW_INFERENCE
fun idAny(x: Any) = x
fun <T> id(x: T) = x
fun idUnit(x: Unit) = x

class MList {
    // MutableCollection<T>.add returns Boolean, but nobody cares
    fun add(): Boolean = true
}
val mlist = MList()

fun work() {}

val xx1 = if (true) 42
val xx2: Unit = if (true) 42
val xx3 = idAny(if (true) 42)
val xx4 = id(if (true) 42)
val xx5 = idUnit(if (true) 42)
val xx6 = null ?: if (true) 42
val xx7 = "" + if (true) 42

val wxx1 = when { true -> 42 }
val wxx2: Unit = when { true -> 42 }
val wxx3 = idAny(when { true -> 42 })
val wxx4 = id(when { true -> 42 })
val wxx5 = idUnit(when { true -> 42 })
val wxx6 = null ?: when { true -> 42 }
val wxx7 = "" + when { true -> 42 }

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

fun f1() = if (true) work()
fun f2() = if (true) mlist.add()
fun f3() = if (true) 42
fun f4(): Unit = if (true) work()
fun f5(): Unit = if (true) mlist.add()
fun f6(): Unit = if (true) 42
fun g1() = when { true -> work() }
fun g2() = when { true -> mlist.add() }
fun g3() = when { true -> 42 }
fun g4(): Unit = when { true -> work() }
fun g5(): Unit = when { true -> mlist.add() }
fun g6(): Unit = when { true -> 42 }

fun foo1(x: String?) {
    "" + if (true) 42
    w@while (true) {
        x ?: if (true) break
        x ?: when { true -> break@w }
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

