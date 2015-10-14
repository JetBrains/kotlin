import kotlin.jvm.Synchronized

interface My {
    <!SYNCHRONIZED_ON_ABSTRACT!>@Synchronized<!> fun foo()

    @Synchronized fun bar() = 1

    @Synchronized fun baz(): String {
        return "abc"
    }
}

abstract class Your {
    <!SYNCHRONIZED_ON_ABSTRACT!>@Synchronized<!> abstract fun foo()

    @Synchronized fun bar() = 1

    @Synchronized open fun baz(): String {
        return "xyz"
    }
}

@Synchronized fun gav() = 1
