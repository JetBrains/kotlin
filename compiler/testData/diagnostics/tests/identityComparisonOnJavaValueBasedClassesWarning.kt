// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

fun test(ld: java.time.LocalDate?, ld2: java.time.LocalDate) {
    ld === ld2
    ld !== ld2
    ld === null
    ld as Any === ld2 as Any
    ld === Any()
    Any() === ld
}

fun getVersion(): Runtime.Version {
    return Runtime.version()
}

fun testReturnVal() {
    getVersion() === getVersion()
}

fun testLambda() {
    val version = getVersion()
    val lambda = {
        version === getVersion()
    }
}

fun testMultiple(x: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Integer<!>) {
    x === x && x === x
}
