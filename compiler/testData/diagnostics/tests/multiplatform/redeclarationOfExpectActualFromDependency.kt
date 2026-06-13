// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect internal fun foo()

// MODULE: lib-platform()()(lib-common)
actual internal fun foo() {}

// MODULE: app-common(lib-common)
expect internal fun foo()

private fun test() {
    // should resolve to expect foo from app-common
    foo()
}

// MODULE: app-platform(lib-platform)()(app-common)
actual internal fun foo() {}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration */
