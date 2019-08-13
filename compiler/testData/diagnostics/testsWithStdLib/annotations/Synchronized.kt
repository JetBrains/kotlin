import kotlin.jvm.Synchronized

interface My {
    <!SYNCHRONIZED_IN_INTERFACE!>@Synchronized<!> fun foo()

    <!SYNCHRONIZED_IN_INTERFACE!>@Synchronized<!> fun bar() = 1

    <!SYNCHRONIZED_IN_INTERFACE!>@Synchronized<!> fun baz(): String {
        return "abc"
    }

    var v: String
        <!SYNCHRONIZED_IN_INTERFACE!>@Synchronized<!> get() = ""
        <!SYNCHRONIZED_IN_INTERFACE!>@Synchronized<!> set(<!UNUSED_PARAMETER!>value<!>) {}
}

abstract class Your {
    <!SYNCHRONIZED_ON_ABSTRACT!>@Synchronized<!> abstract fun foo()

    @Synchronized fun bar() = 1

    @Synchronized open fun baz(): String {
        return "xyz"
    }

    var v: String
        @Synchronized get() = ""
        @Synchronized set(<!UNUSED_PARAMETER!>value<!>) {}
}

@Synchronized fun gav() = 1
