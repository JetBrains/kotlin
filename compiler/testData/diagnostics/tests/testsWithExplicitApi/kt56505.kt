// FIR_IDENTICAL
// SKIP_TXT
// ISSUE: KT-56505

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun f1<!>() {
    class LocalClass {
        public var foo = 0
    }
    LocalClass().foo = 1
}

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>class J1<!> {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>var buf<!>: Int = 0
        <!GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY!>private<!> get() = 42
        protected set(value) {
            field = value
        }
}
