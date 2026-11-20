// ISSUE: KT-90782

// Top-level script classes are in statement position just like local classes
class TopLevel
error("boom")

class TopLevelWithConstructorOnNextLine
private constructor()

fun foo() {
    class Local
    error("boom")
}

run {
    class LocalInLambda
    error("boom")
}

// Member classes are not affected
class Outer {
    class Nested
    private (x: Int)
}
