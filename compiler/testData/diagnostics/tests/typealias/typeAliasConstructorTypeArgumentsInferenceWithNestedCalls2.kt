// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

interface Ref<T> {
    var x: T
}

class LateInitNumRef<NN: Number>() : Ref<NN> {
    constructor(x: NN) : this() { this.x = x }

    private var xx: NN? = null

    override var x: NN
        get() = xx!!
        set(value) {
            xx = value
        }
}

typealias LateNR<Nt> = LateInitNumRef<Nt>

fun <V, R : Ref<in V>> update(r: R, v: V): R {
    r.x = v
    return r
}

val r1 = update(LateInitNumRef(), 1)
val r1a = update(LateNR(), 1)
val r2 = update(LateInitNumRef(1), 1)
val r2a = update(LateNR(1), 1)
val r3 = LateInitNumRef(1)
val r3a = LateNR(1)

fun test() {
    r1.x = <!OI;TYPE_MISMATCH!>r1.x<!>
    r1a.x = <!OI;TYPE_MISMATCH!>r1a.x<!>
    r2.x = r2.x
    r2a.x = r2a.x
    r3.x = r3.x
    r3a.x = r3a.x
}