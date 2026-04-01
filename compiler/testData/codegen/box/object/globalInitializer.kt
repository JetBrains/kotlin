// Does not fail with TR.

public val z: Any = Z

private object Z

fun box(): String {
    if (z is Z)
        return "OK"
    else
        return "FAIL"
}
