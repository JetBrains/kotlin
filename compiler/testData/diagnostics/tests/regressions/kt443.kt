// KT-443 Write allowed to super.val

open class M() {
    open val b: Int = 5
}

class N() : M() {
    val a : Int
        get() {
            <!VAL_REASSIGNMENT!>super.b<!> = super.b + 1
            return super.b + 1
        }
    override val b: Int = a + 1
}
