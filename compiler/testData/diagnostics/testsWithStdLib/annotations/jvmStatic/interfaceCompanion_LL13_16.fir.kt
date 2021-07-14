// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: +JvmStaticInInterface
// !JVM_TARGET: 1.6

interface B {
    companion object {
        @JvmStatic fun a1() {

        }

        @JvmStatic private fun a2() {

        }

        @JvmStatic protected fun a3() {

        }

        @JvmStatic internal fun a4() {

        }

        @JvmStatic external fun a5()

        @JvmStatic
        var foo = 1

        @JvmStatic
        var foo1 = 1
            protected set

        @JvmStatic
        var foo2 = 1
            private set

        @JvmStatic
        private var foo3 = 1

        @JvmStatic
        protected var foo4 = 1

        @JvmStatic
        protected var foo5 = 1

        @JvmStatic
        val foo6 = 1

        val foo7 = 1
        @JvmStatic get

        private var foo8 = 1
        @JvmStatic <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set

        public var foo9 = 1
        @JvmStatic private set

        @JvmStatic
        val foo10: Int external get

        val foo11: Int @JvmStatic external get
    }

}