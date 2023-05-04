// !DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// a = final + not initialized in place + deferred init
// e = final + not initialized in place
// c = final + initialized in place

// b = open + not initialized in place + deferred init
// f = open + not initialized in place
// d = open + initialized in place
class Foo : I {
    // no getter
                                                         val a0: Int
                   <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val e0: Int<!>
                                                         val c0: Int = 1
                                                <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>override val b0: Int<!>
          <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override val f0: Int<!>
                                                override val d0: Int = 1

    // getter with field
                                                val a1: Int; get() = field
                         <!MUST_BE_INITIALIZED!>val e1: Int<!>; get() = field
                                                val c1: Int = 1; get() = field
                                       <!MUST_BE_INITIALIZED_OR_BE_FINAL!>override val b1: Int<!>; get() = field
                <!MUST_BE_INITIALIZED!>override val f1: Int<!>; get() = field
                                       override val d1: Int = 1; get() = field

    // getter with empty body
                                                         val a2: Int; get
                   <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val e2: Int<!>; get
                                                         val c2: Int = 1; get
                                                <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>override val b2: Int<!>; get
          <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override val f2: Int<!>; get
                                                override val d2: Int = 1; get

    // getter no field
             val a3: Int; get() = 1
             val e3: Int; get() = 1
             val c3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; get() = 1
    override val b3: Int; get() = 1
    override val f3: Int; get() = 1
    override val d3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; get() = 1

    init {
        a0 = 1
        a1 = 1
        a2 = 1
        <!VAL_REASSIGNMENT!>a3<!> = 1

        b0 = 1
        b1 = 1
        b2 = 1
        <!VAL_REASSIGNMENT!>b3<!> = 1
    }
}

interface I {
    val b0: Int
    val b1: Int
    val b2: Int
    val b3: Int

    val f0: Int
    val f1: Int
    val f2: Int
    val f3: Int

    val d0: Int
    val d1: Int
    val d2: Int
    val d3: Int
}
