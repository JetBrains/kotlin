// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS










//////////////////////////////////////////////////////////////////////////////////////////////////
/// READ THIS TEST AS A TABLE IN IDE (It may be not correctly displayed in Space or GitHub)!!! ///
//////////////////////////////////////////////////////////////////////////////////////////////////











// a = final + not initialized in place + deferred init
// e = final + not initialized in place
// c = final + initialized in place

// b = open + not initialized in place + deferred init
// f = open + not initialized in place
// d = open + initialized in place
class Foo : I {
    //                                             no setter;                                                                 setter with field;                                                                       setter with empty body;                                                        setter no field;
    // no getter
                                                   var a00: Int;                                <!MUST_BE_INITIALIZED!>var a01: Int<!>; set(v) { field = v };                                                               var a02: Int; set;                              <!MUST_BE_INITIALIZED!>var a03: Int<!>; set(v) {};
             <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var e00: Int<!>;                             <!MUST_BE_INITIALIZED!>var e01: Int<!>; set(v) { field = v };                         <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var e02: Int<!>; set;                           <!MUST_BE_INITIALIZED!>var e03: Int<!>; set(v) {};
                                                   var c00: Int = 1;                                                   var c01: Int = 1; set(v) { field = v };                                                              var c02: Int = 1; set;                                                 var c03: Int = 1; set(v) {};
                                          override var b00: Int;                       <!MUST_BE_INITIALIZED!>override var b01: Int<!>; set(v) { field = v };                                                      override var b02: Int; set;                     <!MUST_BE_INITIALIZED!>override var b03: Int<!>; set(v) {};
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override var f00: Int<!>;                    <!MUST_BE_INITIALIZED!>override var f01: Int<!>; set(v) { field = v };                <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override var f02: Int<!>; set;                  <!MUST_BE_INITIALIZED!>override var f03: Int<!>; set(v) {};
                                          override var d00: Int = 1;                                          override var d01: Int = 1; set(v) { field = v };                                                     override var d02: Int = 1; set;                                        override var d03: Int = 1; set(v) {};
    // getter with field
                                                   var a10: Int; get() = field;                 <!MUST_BE_INITIALIZED!>var a11: Int<!>; set(v) { field = v } get() = field;                                                 var a12: Int; set get() = field;                <!MUST_BE_INITIALIZED!>var a13: Int<!>; set(v) {} get() = field;
                            <!MUST_BE_INITIALIZED!>var e10: Int<!>; get() = field;              <!MUST_BE_INITIALIZED!>var e11: Int<!>; set(v) { field = v } get() = field;                          <!MUST_BE_INITIALIZED!>var e12: Int<!>; set get() = field;             <!MUST_BE_INITIALIZED!>var e13: Int<!>; set(v) {} get() = field;
                                                   var c10: Int = 1; get() = field;                                    var c11: Int = 1; set(v) { field = v } get() = field;                                                var c12: Int = 1; set get() = field;                                   var c13: Int = 1; set(v) {} get() = field;
                                          override var b10: Int; get() = field;        <!MUST_BE_INITIALIZED!>override var b11: Int<!>; set(v) { field = v } get() = field;                                        override var b12: Int; set get() = field;       <!MUST_BE_INITIALIZED!>override var b13: Int<!>; set(v) {} get() = field;
                   <!MUST_BE_INITIALIZED!>override var f10: Int<!>; get() = field;     <!MUST_BE_INITIALIZED!>override var f11: Int<!>; set(v) { field = v } get() = field;                 <!MUST_BE_INITIALIZED!>override var f12: Int<!>; set get() = field;    <!MUST_BE_INITIALIZED!>override var f13: Int<!>; set(v) {} get() = field;
                                          override var d10: Int = 1; get() = field;                           override var d11: Int = 1; set(v) { field = v } get() = field;                                       override var d12: Int = 1; set get() = field;                          override var d13: Int = 1; set(v) {} get() = field;
    // getter with empty body
                                                   var a20: Int; get;                           <!MUST_BE_INITIALIZED!>var a21: Int<!>; set(v) { field = v } get;                                                           var a22: Int; set get;                          <!MUST_BE_INITIALIZED!>var a23: Int<!>; set(v) {} get;
             <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var e20: Int<!>; get;                        <!MUST_BE_INITIALIZED!>var e21: Int<!>; set(v) { field = v } get;                     <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var e22: Int<!>; set get;                       <!MUST_BE_INITIALIZED!>var e23: Int<!>; set(v) {} get;
                                                   var c20: Int = 1; get;                                              var c21: Int = 1; set(v) { field = v } get;                                                          var c22: Int = 1; set get;                                             var c23: Int = 1; set(v) {} get;
                                          override var b20: Int; get;                  <!MUST_BE_INITIALIZED!>override var b21: Int<!>; set(v) { field = v } get;                                                  override var b22: Int; set get;                 <!MUST_BE_INITIALIZED!>override var b23: Int<!>; set(v) {} get;
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override var f20: Int<!>; get;               <!MUST_BE_INITIALIZED!>override var f21: Int<!>; set(v) { field = v } get;            <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override var f22: Int<!>; set get;              <!MUST_BE_INITIALIZED!>override var f23: Int<!>; set(v) {} get;
                                          override var d20: Int = 1; get;                                     override var d21: Int = 1; set(v) { field = v } get;                                                 override var d22: Int = 1; set get;                                    override var d23: Int = 1; set(v) {} get;
    // getter no field
                                                   var a30: Int; get() = 1;                     <!MUST_BE_INITIALIZED!>var a31: Int<!>; set(v) { field = v } get() = 1;                                                     var a32: Int; set get() = 1;                                           var a33: Int; set(v) {} get() = 1;
                            <!MUST_BE_INITIALIZED!>var e30: Int<!>; get() = 1;                  <!MUST_BE_INITIALIZED!>var e31: Int<!>; set(v) { field = v } get() = 1;                              <!MUST_BE_INITIALIZED!>var e32: Int<!>; set get() = 1;                                        var e33: Int; set(v) {} get() = 1;
                                                   var c30: Int = 1; get() = 1;                                        var c31: Int = 1; set(v) { field = v } get() = 1;                                                    var c32: Int = 1; set get() = 1;                                       var c33: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; set(v) {} get() = 1;
                                          override var b30: Int; get() = 1;            <!MUST_BE_INITIALIZED!>override var b31: Int<!>; set(v) { field = v } get() = 1;                                            override var b32: Int; set get() = 1;                                  override var b33: Int; set(v) {} get() = 1;
                   <!MUST_BE_INITIALIZED!>override var f30: Int<!>; get() = 1;         <!MUST_BE_INITIALIZED!>override var f31: Int<!>; set(v) { field = v } get() = 1;                     <!MUST_BE_INITIALIZED!>override var f32: Int<!>; set get() = 1;                               override var f33: Int; set(v) {} get() = 1;
                                          override var d30: Int = 1; get() = 1;                               override var d31: Int = 1; set(v) { field = v } get() = 1;                                           override var d32: Int = 1; set get() = 1;                              override var d33: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; set(v) {} get() = 1;

    init {
        a00 = 1
        a01 = 1
        a02 = 1
        a03 = 1
        a10 = 1
        a11 = 1
        a12 = 1
        a13 = 1
        a20 = 1
        a21 = 1
        a22 = 1
        a23 = 1
        a30 = 1
        a31 = 1
        a32 = 1
        a33 = 1

        b00 = 1
        b01 = 1
        b02 = 1
        b03 = 1
        b10 = 1
        b11 = 1
        b12 = 1
        b13 = 1
        b20 = 1
        b21 = 1
        b22 = 1
        b23 = 1
        b30 = 1
        b31 = 1
        b32 = 1
        b33 = 1
    }
}

interface I {
    val b00: Int
    val b01: Int
    val b02: Int
    val b03: Int
    val b10: Int
    val b11: Int
    val b12: Int
    val b13: Int
    val b20: Int
    val b21: Int
    val b22: Int
    val b23: Int
    val b30: Int
    val b31: Int
    val b32: Int
    val b33: Int

    val f00: Int
    val f01: Int
    val f02: Int
    val f03: Int
    val f10: Int
    val f11: Int
    val f12: Int
    val f13: Int
    val f20: Int
    val f21: Int
    val f22: Int
    val f23: Int
    val f30: Int
    val f31: Int
    val f32: Int
    val f33: Int

    val d00: Int
    val d01: Int
    val d02: Int
    val d03: Int
    val d10: Int
    val d11: Int
    val d12: Int
    val d13: Int
    val d20: Int
    val d21: Int
    val d22: Int
    val d23: Int
    val d30: Int
    val d31: Int
    val d32: Int
    val d33: Int
}
