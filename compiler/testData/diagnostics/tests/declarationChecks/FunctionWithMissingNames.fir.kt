// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class a
interface A
interface B

<!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
<!FUNCTION_DECLARATION_WITH_NO_NAME!>fun A.()<!> {}

@a <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
<!FUNCTION_DECLARATION_WITH_NO_NAME!>fun @a A.()<!> {}

class Outer {
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun B.()<!> {}

    @a <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun @a A.()<!> {}
}

fun outerFun() {
    fun () {}
    fun B.() {}

    @a fun () {}
    fun @a A.() {}
}