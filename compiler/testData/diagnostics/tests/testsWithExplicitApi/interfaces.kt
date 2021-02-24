// SKIP_TXT

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>interface I1<!> {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun i<!>()
}

public interface I2 {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun i<!>()
}

public interface I3 {
    public fun i()
    public val v: Int
}

public interface I4 {
    public fun i(): Int
    public val v: Int
}

public class Impl: I3 {
    override fun i() {}
    override val v: Int
        get() = 10
}

public class Impl2: I4 {
    override fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>i<!>() = 10
    override val <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>v<!> = 10
}

private class PrivateImpl: I4 {
    override fun i() = 10
    override val v = 10
}
