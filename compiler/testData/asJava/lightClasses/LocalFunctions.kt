// p.A

package p

class A {
    init {
        fun localFunInInit() {}
    }

    constructor(x: Int) {
        fun localFunInConstructor() {}
    }

    fun memberFun() {
        fun localFunInMemberFun() {}
    }

    val property: Int
        get() {
            fun localFunInPropertyAccessor() {}
            return 1
        }
}

// FIR_COMPARISON
