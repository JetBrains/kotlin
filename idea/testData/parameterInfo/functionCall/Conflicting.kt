// Possible parameter info, for index, lambda and arguments.
// Test is flaky without the fix. In IDEA there's a blinking without the fix with different popups.

fun foo(a: A) {
    a.subscribe(object : L {
        override fun on() {
            withLambda {
                <caret>arrayOf(1)[0]
            }
        }
    })
}

fun withLambda(a: (Int) -> Unit) {}

interface L {
    fun on()
}

class A {
    fun subscribe(listener: L) {}
}

/*
Text: (<highlight>a: (Int) -> Unit</highlight>), Disabled: false, Strikeout: false, Green: true
*/

