@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class a
interface A
interface B

<!CONFLICTING_OVERLOADS, FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
<!CONFLICTING_OVERLOADS, FUNCTION_DECLARATION_WITH_NO_NAME!>fun A.()<!> {}

<!CONFLICTING_OVERLOADS, FUNCTION_DECLARATION_WITH_NO_NAME!>@a fun ()<!> {}
<!CONFLICTING_OVERLOADS, FUNCTION_DECLARATION_WITH_NO_NAME!>fun @a A.()<!> {}

class Outer {
    <!CONFLICTING_OVERLOADS, FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun B.()<!> {}

    <!CONFLICTING_OVERLOADS, FUNCTION_DECLARATION_WITH_NO_NAME!>@a fun ()<!> {}
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun @a A.()<!> {}
}

fun outerFun() {
    fun () {}
    fun B.() {}

    @a fun () {}
    fun @a A.() {}
}
