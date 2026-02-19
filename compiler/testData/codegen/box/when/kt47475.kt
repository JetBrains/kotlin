// TARGET_BACKEND: JVM
// CHECK_BYTECODE_TEXT

// MODULE: lib
// FILE: lib.kt

enum class SomeEnum{A, B}

inline fun inlineEnumWhen(someEnum: SomeEnum) = when(someEnum) {
    SomeEnum.A -> "A"
    else -> "not A"
}

// 2 INNERCLASS

// MODULE: caller(lib)
// FILE: caller.kt

fun box(): String {
    inlineEnumWhen(SomeEnum.A)
    val mappings = Class.forName("CallerKt\$box\$\$inlined\$inlineEnumWhen\$1\$wm\$LibKt\$WhenMappings")
    return if (mappings.enclosingClass == null) "OK" else "FAIL"
}

// 0 INNERCLASS
