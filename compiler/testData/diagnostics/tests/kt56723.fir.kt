import kotlin.reflect.KProperty

fun foo(f: () -> Unit) {
    f()
}

class Wrapper(var s: String)

fun bar(w: Wrapper?) {
    // K1: type is () -> Unit, K2: type is () -> Unit?
    val lambda = {
        w?.s = "X"
    }
    // K1: Ok, K2: ARGUMENT_TYPE_MISMATCH
    foo(lambda)
}

class Wrapper2(val w: Wrapper?)

fun baz(w2: Wrapper2?) {
    val lambda = {
        w2?.w?.s = "X"
    }
    foo(lambda)
}

object Indexible {
    operator fun get(index: Int) = "$index"
    operator fun set(index: Int, value: String) {}
}
class IndexibleRef(val ind: Indexible)
class IndexibleRefRef(val ref: IndexibleRef?)

fun ban(refRef: IndexibleRefRef?, ref: IndexibleRef?) {
    val lambda = {
        ref?.ind[1] = "X"
    }
    foo(lambda)

    val lambda2 = {
        refRef?.ref?.ind[1] = "X"
    }
    foo(lambda2)

    val lambda3 = {
        ref?.ind?.set(1, "X")
    }
    foo(<!ARGUMENT_TYPE_MISMATCH!>lambda3<!>)

    val lambda4 = {
        refRef?.ref?.ind?.set(1, "X")
    }
    foo(<!ARGUMENT_TYPE_MISMATCH!>lambda4<!>)
}

object PlusAssignable {
    operator fun plusAssign(index: Int) {}
}

object Indexible2 {
    operator fun get(index: Int) = PlusAssignable
    operator fun set(index: Int, value: String) {}
}

class Indexible2Ref(val ind: Indexible2)

fun bam(ref: Indexible2Ref?) {
    val lambda = {
        ref?.ind[1] += 1
    }
    foo(lambda)

    val lambd2 = {
        ref?.ind?.get(1)?.plusAssign(1)
    }
    foo(lambda)
}

class DelegatedHolder {
    var delegated by object {
        operator fun getValue(thisRef: Any?, desc: KProperty<*>) = "test"
        operator fun setValue(thisRef: Any?, desc: KProperty<*>, value: String) {}
    }
}

fun bap(holder: DelegatedHolder?) {
    val lambda = {
        holder?.delegated = "Y"
    }
    foo(lambda)
}
