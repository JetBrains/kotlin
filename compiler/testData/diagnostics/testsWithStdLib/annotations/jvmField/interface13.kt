// !LANGUAGE: +JvmFieldInInterface

interface A {

    companion object {
        @JvmField
        val c = 3
    }
}


interface B {

    companion object {
        @JvmField
        val c = 3

        @JvmField
        val a = 3
    }
}

interface C {
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val c = 3

        val a = 3
    }
}

interface D {
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        var c = 3
    }
}


interface E {
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        private val a = 3

        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        internal val b = 3

        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        protected val c = 3
    }
}


interface F {
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> val a = 3
    }
}
