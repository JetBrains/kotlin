fun Any.get(a: Int) {
    if (a > 0) {
        <lineMarker descr="Recursive call">this[a - 1]</lineMarker>
    }
}

class A {
    override fun <lineMarker descr="Overrides function in 'Any'"></lineMarker>equals(other: Any?): Boolean {
        this <lineMarker descr="Recursive call">==</lineMarker> other
        return true
    }

    fun inc(): A {
        this<lineMarker descr="Recursive call">++</lineMarker>
        <lineMarker descr="Recursive call">++</lineMarker>this
        return this
    }

    fun component1(): Int {
        // TODO: should be recursion marker too
        val (a) = this
        return 1
    }

    fun plus() {
        <lineMarker descr="Recursive call">+</lineMarker>this
    }

    fun minus() {
        <lineMarker descr="Recursive call">-</lineMarker>this
    }

    fun plus(a: Int) {
        this <lineMarker descr="Recursive call">+</lineMarker> 1
        this += 1
    }

    fun invoke() {
        val a = A()
        a()
        a.invoke()

        this.<lineMarker descr="Recursive call">invoke</lineMarker>()
        <lineMarker descr="Recursive call">this</lineMarker>()
    }
}

class B
fun B.invoke() {
    <lineMarker descr="Recursive call">this</lineMarker>()
    <lineMarker descr="Recursive call">invoke</lineMarker>()
}