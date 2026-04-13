// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun localFun() {
    fun local(): Int = 123
    @IgnorableReturnValue fun localIgnorable() = ""
    <!RETURN_VALUE_NOT_USED!>local<!>()
    localIgnorable()
}

class Outer {
    fun foo(): String {
        class Inner {
            @IgnorableReturnValue fun bar() {
                fun local() = ""
                <!RETURN_VALUE_NOT_USED!>local<!>()
            }
            fun inner() = ""
        }
        <!RETURN_VALUE_NOT_USED!>Inner<!>()
        Inner().<!RETURN_VALUE_NOT_USED!>inner<!>()
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
