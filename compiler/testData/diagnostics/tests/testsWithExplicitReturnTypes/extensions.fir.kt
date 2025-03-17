// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

public val Int.property get() = true
public fun Int.f() = 42

public class C {
    public val Int.property get() = true
    public fun Int.f() = 42
}

// Reports `NO_EXPLICIT_RETURN_TYPE_IN_API_MODE` even for declarations with `DeprecationLevel.ERROR`
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This function is prohibited",
)
public fun Int.deprecatedFunction() = 42