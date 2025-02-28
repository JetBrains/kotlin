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
