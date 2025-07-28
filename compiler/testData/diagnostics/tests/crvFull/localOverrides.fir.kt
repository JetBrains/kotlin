// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun main() {
    val myList = object : AbstractMutableList<String>() {
        override val size: Int get() = TODO("Not yet implemented")

        override fun get(index: Int): String = TODO()

        override fun set(index: Int, element: String): String = TODO()

        override fun removeAt(index: Int): String = TODO()

        override fun add(index: Int, element: String) = TODO()
    }

    <!RETURN_VALUE_NOT_USED!>myList.size<!>
    <!RETURN_VALUE_NOT_USED!>myList.get(0)<!>
    myList.set(0, "")
}

fun main2() {
    val myList = @MustUseReturnValue object : AbstractMutableList<String>() {
        override val size: Int get() = TODO("Not yet implemented")

        @IgnorableReturnValue override fun get(index: Int): String = TODO()

        override fun <!OVERRIDING_IGNORABLE_WITH_MUST_USE!>set<!>(index: Int, element: String): String = TODO()

        override fun <!OVERRIDING_IGNORABLE_WITH_MUST_USE!>removeAt<!>(index: Int): String = TODO()

        override fun add(index: Int, element: String) = TODO()
    }
    <!RETURN_VALUE_NOT_USED!>myList.size<!>
    myList.get(0)
    <!RETURN_VALUE_NOT_USED!>myList.set(0, "")<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, getter, integerLiteral, localProperty, operator,
override, propertyDeclaration, stringLiteral */
