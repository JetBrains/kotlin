// RUN_PIPELINE_TILL: FRONTEND

class Example {
    static fun invoke(x: Int): Example = Example()
}

val callConstructor = Example()
val callInvoke = Example(<!TOO_MANY_ARGUMENTS!>3<!>)
val callWrong = Example(<!TOO_MANY_ARGUMENTS!>true<!>)
