operator fun Any.get(a: Int) {
    if (a > 0) {
        <lineMarker descr="Recursive call">this[a - 1]</lineMarker>
    }
}

class A {
    override fun <lineMarker descr="Overrides function in 'Any'">equals</lineMarker>(other: Any?): Boolean {
        this <lineMarker descr="Recursive call">==</lineMarker> other
        return true
    }

    operator fun inc(): A {
        this<lineMarker descr="Recursive call">++</lineMarker>
        <lineMarker descr="Recursive call">++</lineMarker>this
        return this
    }

    operator fun component1(): Int {
        // TODO: should be recursion marker too
        val (a) = this
        return 1
    }

    operator fun unaryPlus() {
        <lineMarker descr="Recursive call">+</lineMarker>this
    }

    operator fun unaryMinus() {
        <lineMarker descr="Recursive call">-</lineMarker>this
    }

    operator fun plus(a: Int) {
        this <lineMarker descr="Recursive call">+</lineMarker> 1
        this += 1
    }

    operator fun invoke() {
        val a = A()
        a()
        a.invoke()

        this.<lineMarker descr="Recursive call">invoke</lineMarker>()
        <lineMarker descr="Recursive call">this</lineMarker>()
    }
}

class B
operator fun B.invoke() {
    <lineMarker descr="Recursive call">this</lineMarker>()
    <lineMarker descr="Recursive call">invoke</lineMarker>()
}