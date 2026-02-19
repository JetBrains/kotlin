class BC<A: Any>(val f: A) {
    open fun m(): Boolean {
        val cond: Boolean = BC(2.0).f == 3.0
        return cond
    }
    open fun m1(): Boolean {
        val cond: Boolean = BC(2.0).f == 2.0
        return cond
    }
    open fun m2(): Boolean {
        val cond: Boolean = BC(2.0f).f == 3.0f
        return cond
    }
    open fun m3(): Boolean {
        val cond: Boolean = BC(2.0f).f == 2.0f
        return cond
    }
    open fun m4(): Boolean {
        val cond: Boolean = BC(2).f == 3
        return cond
    }
    open fun m5(): Boolean {
        val cond: Boolean = BC(2).f == 2
        return cond
    }
}

class BC1<E: Double, A: Any>(val f: A) {
    open fun m(p: E): Boolean {
        val obj = BC1<E, E>(p)
        val cond = (obj.f == p)
        return cond
    }
}

class BC2<E: Float, A: Any>(val f: A) {
    open fun m(p: E): Boolean {
        val obj = BC2<E, E>(p)
        val cond = (obj.f == p)
        return cond
    }
}

class BC3<E: Int, A: Any>(val f: A) {
    open fun m(p: E): Boolean {
        val obj = BC3<E, E>(p)
        val cond = (obj.f == p)
        return cond
    }
}

fun box(): String {
    if (BC(Any()).m()) return "NOT_OK1"
    if (!BC(Any()).m1()) return "NOT_OK2"
    if (BC(Any()).m2()) return "NOT_OK3"
    if (!BC(Any()).m3()) return "NOT_OK4"
    if (BC(Any()).m4()) return "NOT_OK5"
    if (!BC(Any()).m5()) return "NOT_OK6"
    if (!BC1<Double, _>(Any()).m(2.0)) return "NOT_OK7"
    if (!BC2<Float, _>(Any()).m(2.0f)) return "NOT_OK8"
    if (!BC3<Int, _>(Any()).m(2)) return "NOT_OK9"
    BC1<Nothing, _>(2.0)
    BC2<Nothing, _>(2.0f)
    BC3<Nothing, _>(2)
    return "OK"
}
