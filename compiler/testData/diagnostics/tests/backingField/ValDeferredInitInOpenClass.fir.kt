// !DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// a = final + not initialized in place + deferred init
// e = final + not initialized in place
// c = final + initialized in place

// b = open + not initialized in place + deferred init
// f = open + not initialized in place
// d = open + initialized in place
open class Foo {
    // no getter
                                                     val a0: Int
               <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val e0: Int<!>
                                                     val c0: Int = 1
                                                <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open val b0: Int<!>
          <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open val f0: Int<!>
                                                open val d0: Int = 1

    // getter with field
                                            val a1: Int; get() = field
                     <!MUST_BE_INITIALIZED!>val e1: Int<!>; get() = field
                                            val c1: Int = 1; get() = field
                                       <!MUST_BE_INITIALIZED!>open val b1: Int<!>; get() = field
                <!MUST_BE_INITIALIZED!>open val f1: Int<!>; get() = field
                                       open val d1: Int = 1; get() = field

    // getter with empty body
                                                     val a2: Int; get
               <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val e2: Int<!>; get
                                                     val c2: Int = 1; get
                                                <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open val b2: Int<!>; get
          <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open val f2: Int<!>; get
                                                open val d2: Int = 1; get

    // getter no field
         val a3: Int; get() = 1
         val e3: Int; get() = 1
         val c3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; get() = 1
    open val b3: Int; get() = 1
    open val f3: Int; get() = 1
    open val d3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; get() = 1

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
