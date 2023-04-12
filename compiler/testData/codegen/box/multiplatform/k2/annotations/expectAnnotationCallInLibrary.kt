// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// ISSUE: KT-57963

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

@file:Ann

@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.LOCAL_VARIABLE
)
expect annotation class Ann constructor()

@Ann
class C<@Ann T> {
    fun ok() = "OK"
}

@Ann
fun f(@Ann p: TA): TA {
    @Ann
    var localVar = p
    return p
}

@field:Ann
@get:Ann
@set:Ann
var ok = "OK"

@Ann
val variable = "OK"

enum class E {
    @Ann
    OK
}

@Ann
typealias TA = String

// MODULE: lib()()(common)
// FILE: lib.kt

@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.LOCAL_VARIABLE
)
actual annotation class Ann

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    if (C<String>().ok() != "OK") return "FAIL 1"
    if (f("OK") != "OK") return "FAIL 2"
    if (ok != "OK") return "FAIL 3"
    if (E.OK.name != "OK") return "FAIL 4"

    return "OK"
}