// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

public val Int.<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>property<!> get() = true
public fun Int.<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>f<!>() = 42

public class C {
    public val Int.<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>property<!> get() = true
    public fun Int.<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>f<!>() = 42
}

// Reports `NO_EXPLICIT_RETURN_TYPE_IN_API_MODE` even for declarations with `DeprecationLevel.ERROR`
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This function is prohibited",
)
public fun Int.<!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>deprecatedFunction<!>() = 42
