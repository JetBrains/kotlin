// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun localFun() {
    fun local(): Int = 123
    @IgnorableReturnValue fun localIgnorable() = ""
    local()
    localIgnorable()
}

class Outer {
    fun foo(): String {
        class Inner {
            @IgnorableReturnValue fun bar() {
                fun local() = ""
                local()
            }
            fun inner() = ""
        }
        Inner()
        Inner().inner()
        Inner().bar()
        return ""
    }

    fun bar(): String = ""
}

fun main() {
    Outer().<!RETURN_VALUE_NOT_USED!>foo<!>()
    Outer().<!RETURN_VALUE_NOT_USED!>bar<!>()
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, functionDeclaration, integerLiteral, localFunction */
