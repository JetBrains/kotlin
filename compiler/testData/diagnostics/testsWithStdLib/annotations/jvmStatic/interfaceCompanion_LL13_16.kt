// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: +JvmStaticInInterface
interface B {
    companion object {
        <!JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic fun a1()<!> {

        }

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic private fun a2()<!> {

        }

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic protected fun a3()<!> {

        }

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic internal fun a4()<!> {

        }

        <!JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        var foo<!> = 1

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        var foo1<!> = 1
            protected set

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        var foo2<!> = 1
            private set

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        private var foo3<!> = 1

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        protected var foo4<!> = 1

        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        protected var foo5<!> = 1

        <!JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic
        val foo6<!> = 1

        val foo7 = 1
        <!JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic get<!>

        private var foo8 = 1
        <!JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set<!>

        public var foo9 = 1
        <!JVM_STATIC_ON_NON_PUBLIC_MEMBER, JVM_STATIC_IN_INTERFACE_1_6!>@JvmStatic private set<!>

    }

}