// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
class Foo : I {
    // no getter
    val final_notInitializedInPlace_deferredInit0: Int
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val final_notInitializedInPlace0: Int<!>
    val final_initializedInPlace0: Int = 1
    override val open_notInitializedInPlace_deferredInit0: Int
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override val open_notInitializedInPlace0: Int<!>
    override val open_initializedInPlace0: Int = 1

    // getter with field
    val final_notInitializedInPlace_deferredInit1: Int; get() = field
    <!MUST_BE_INITIALIZED!>val final_notInitializedInPlace1: Int<!>; get() = field
    val final_initializedInPlace1: Int = 1; get() = field
    override val open_notInitializedInPlace_deferredinit1: Int; get() = field
    <!MUST_BE_INITIALIZED!>override val open_notInitializedInPlace1: Int<!>; get() = field
    override val open_initializedInPlace1: Int = 1; get() = field

    // getter with empty body
    val final_notInitializedInPlace_deferredInit2: Int; get
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val final_notInitializedInPlace2: Int<!>; get
    val final_initializedInPlace2: Int = 1; get
    override val open_notInitializedInPlace_deferredinit2: Int; get
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override val open_notInitializedInPlace2: Int<!>; get
    override val open_initializedInPlace2: Int = 1; get

    // getter no field
    val final_notInitializedInPlace_deferredInit3: Int; get() = 1
    val final_notInitializedInPlace3: Int; get() = 1
    val final_initializedInPlace3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; get() = 1
    override val open_notInitializedInPlace_deferredinit3: Int; get() = 1
    override val open_notInitializedInPlace3: Int; get() = 1
    override val open_initializedInPlace3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>; get() = 1

    init {
        final_notInitializedInPlace_deferredInit0 = 1
        final_notInitializedInPlace_deferredInit1 = 1
        final_notInitializedInPlace_deferredInit2 = 1
        <!VAL_REASSIGNMENT!>final_notInitializedInPlace_deferredInit3<!> = 1

        open_notInitializedInPlace_deferredInit0 = 1
        open_notInitializedInPlace_deferredinit1 = 1
        open_notInitializedInPlace_deferredinit2 = 1
        <!VAL_REASSIGNMENT!>open_notInitializedInPlace_deferredinit3<!> = 1
    }
}

interface I {
    val open_notInitializedInPlace_deferredInit0: Int
    val open_notInitializedInPlace_deferredinit1: Int
    val open_notInitializedInPlace_deferredinit2: Int
    val open_notInitializedInPlace_deferredinit3: Int

    val open_notInitializedInPlace0: Int
    val open_notInitializedInPlace1: Int
    val open_notInitializedInPlace2: Int
    val open_notInitializedInPlace3: Int

    val open_initializedInPlace0: Int
    val open_initializedInPlace1: Int
    val open_initializedInPlace2: Int
    val open_initializedInPlace3: Int
}
