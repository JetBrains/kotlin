// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateSecondaryConstructorAction
open class Base(n: Int) {
    constructor(a: Int, b: Int): this(a + b)
}

class Foo : Base {<caret>
    val x = 1

    constructor(t: Int, u: Int) : super(u, t)

    constructor(x: Int) : super(x)

    fun foo() {

    }

    fun bar() {

    }
}