// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: +JvmStaticInInterface
// !JVM_TARGET: 1.8
interface B {
    companion object {
        @JvmStatic fun a1() {

        }

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic private fun a2()<!> {

        }

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic protected fun a3()<!> {

        }

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic internal fun a4()<!> {

        }

        @JvmStatic
        var foo = 1

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic
        var foo1<!> = 1
            protected set

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic
        var foo2<!> = 1
            private set

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic
        private var foo3<!> = 1

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic
        protected var foo4<!> = 1

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic
        protected var foo5<!> = 1

        @JvmStatic
        val foo6 = 1

        val foo7 = 1
        @JvmStatic get

        private var foo8 = 1
        <!ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR!>@JvmStatic<!> <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set

        public var foo9 = 1
        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER!>@JvmStatic private set<!>

    }

}