// SKIP_TXT

public class Foo(<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>val bar<!>: Int, private var bar2: String, internal var bar3: Long, public var bar4: Int) {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>var simple<!>: Int = 10
    public var simple2: Int = 10

    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>val withGetter<!>: Int
        get() = 10

    public val withGetter2: Int
        get() = 10

    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>var getterAndSetter<!>: Int = 10
        get() = field
        set(v) { field = v }

    public var getterAndSetter2: Int = 10
        get() = field
        set(v) { field = v }
}