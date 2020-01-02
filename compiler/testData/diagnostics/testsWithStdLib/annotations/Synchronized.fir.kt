import kotlin.jvm.Synchronized

interface My {
    @Synchronized fun foo()

    @Synchronized fun bar() = 1

    @Synchronized fun baz(): String {
        return "abc"
    }

    var v: String
        @Synchronized get() = ""
        @Synchronized set(value) {}
}

abstract class Your {
    @Synchronized abstract fun foo()

    @Synchronized fun bar() = 1

    @Synchronized open fun baz(): String {
        return "xyz"
    }

    var v: String
        @Synchronized get() = ""
        @Synchronized set(value) {}
}

@Synchronized fun gav() = 1
