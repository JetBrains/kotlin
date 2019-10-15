package nonCapturedThisInLambda

fun main() {
    Foo().bar()
}

class Foo {
    val a = "lorem"
    val b: String
    get() = "ipsum"
    fun bar() {
        {
            //Breakpoint!
            Unit
        }()
    }
}

// PRINT_FRAME

// EXPRESSION: a
// RESULT: "lorem": Ljava/lang/String;

// EXPRESSION: b
// RESULT: "ipsum": Ljava/lang/String;

// EXPRESSION: this
// RESULT: instance of nonCapturedThisInLambda.Foo(id=ID): LnonCapturedThisInLambda/Foo;