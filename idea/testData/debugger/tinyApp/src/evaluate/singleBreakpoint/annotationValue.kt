package annotationValue

annotation class Anno(val value: String)

@Anno("abc")
class SomeClass

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 5
}

// EXPRESSION: (SomeClass::class.java.annotations[0] as Anno).value
// RESULT: "abc": Ljava/lang/String;

