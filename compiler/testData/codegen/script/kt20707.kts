enum class Build { Debug, Release }

fun applySomething(build: Build) = <!WHEN_ON_SEALED_GEEN_ELSE!>when (build) {
    Build.Debug -> "OK"
    Build.Release -> "fail"
}<!>

val rv = applySomething(Build.Debug)

// expected: rv: OK
