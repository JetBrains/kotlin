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


// LIGHT_ELEMENTS_NO_DECLARATION: A.class[_get_property_$localFunInPropertyAccessor;_init_$localFunInConstructor;_init_$localFunInInit;memberFun$localFunInMemberFun]
