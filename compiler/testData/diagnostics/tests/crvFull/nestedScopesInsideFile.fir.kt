// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun localFun() {
    fun local(): Int = 123
    @IgnorableReturnValue fun localIgnorable() = ""
    <!RETURN_VALUE_NOT_USED!>local()<!>
    localIgnorable()
}

class Outer {
    fun foo(): String {
        class Inner {
            @IgnorableReturnValue fun bar() {
                fun local() = ""
                <!RETURN_VALUE_NOT_USED!>local()<!>
            }
            fun inner() = ""
        }
        <!RETURN_VALUE_NOT_USED!>Inner()<!>
        <!RETURN_VALUE_NOT_USED!>Inner().inner()<!>
        Inner().bar()
        return ""
    }

    fun bar(): String = ""
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>Outer().foo()<!>
    <!RETURN_VALUE_NOT_USED!>Outer().bar()<!>
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, functionDeclaration, integerLiteral, localFunction */
