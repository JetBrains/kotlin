// "Replace with '@' annotations in whole project" "true"

annotation class Ann(val x: Int = 1)

@Ann @Ann(2) class MyClass {
    @Ann(3) @Ann /* abc */fun foo(x: @Ann Int) {
        @Ann class Local {
            @Ann init {}

            private @Ann @Ann(4) fun foo() {}
        }

        @Ann var x = 1

        1+ @Ann(5) 2

        3+ @Ann(55)4

        5+ @Ann @Ann(6)/* cde */7

        label@@Ann(7) @Ann for (i in 1..100) {}
    }
}
@Ann(1) @Ann
class Q1

@Ann(2)

@Ann(3)

fun bar() {}
