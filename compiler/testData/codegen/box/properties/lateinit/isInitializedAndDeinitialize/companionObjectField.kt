// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

class Class {
    companion object {
        lateinit var public: String
        private lateinit var private: String

        fun test() {
            if (::public.isInitialized) throw AssertionError("Fail 1")
            public = "OK"
            if (public != "OK") throw AssertionError("Fail 2")

            if (::private.isInitialized) throw AssertionError("Fail 3")
            private = "OK"
            if (private != "OK") throw AssertionError("Fail 4")
        }
    }
}

class Interface {
    companion object {
        lateinit var public: String
        private lateinit var private: String

        fun test() {
            if (::public.isInitialized) throw AssertionError("Fail 5")
            public = "OK"
            if (public != "OK") throw AssertionError("Fail 6")

            if (::private.isInitialized) throw AssertionError("Fail 7")
            private = "OK"
            if (private != "OK") throw AssertionError("Fail 8")
        }
    }
}

fun box(): String {
    Class.test()
    Interface.test()
    return "OK"
}
