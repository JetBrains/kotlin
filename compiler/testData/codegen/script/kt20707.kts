// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

enum class Build { Debug, Release }

fun applySomething(build: Build) = when (build) {
    Build.Debug -> "OK"
    Build.Release -> "fail"
}

val rv = applySomething(Build.Debug)

// expected: rv: OK