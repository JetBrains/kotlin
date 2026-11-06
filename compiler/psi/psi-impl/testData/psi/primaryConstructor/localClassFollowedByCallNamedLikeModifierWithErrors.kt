// COMPILATION_ERRORS
// ISSUE: KT-90782

fun localObject() {
    object O
    private("boom")
}

fun localInterface() {
    interface I
    inline("boom")
}

fun localEnumClass() {
    enum class E
    value {}
}

fun localClassFollowedByMultipleModifierLikeTokens() {
    class C
    public inline("boom")
}

fun localClassFollowedByAnnotationAndModifierLikeCall() {
    class C
    @Ann private (x: Int)
}

fun localClassFollowedByParenthesizedParametersOnNextLine() {
    class C
    private (x: Int)
}

fun localClassFollowedByBlockOnNextLine() {
    class C
    private {}
}

// Modifiers on the same line are still parsed as primary constructor modifiers
fun localClassWithModifiersOnSameLine() {
    class C1 private (x: Int)
    class C2 private {}
    class C3 inline("boom")
    class C4 @Ann (x: Int)
}

class TopLevelSameLine1 private (x: Int)

class TopLevelSameLine2 private {}

class TopLevelNextLine1
private (x: Int)

class TopLevelNextLine2
private {}

class TopLevelNextLine3
@Ann (x: Int)

// Member classes are not affected by KT-90782
class Outer {
    class NestedNextLine1
    private (x: Int)

    class NestedNextLine2
    inline("boom")

    fun foo() {
        class LocalInsideMember
        inline("boom")
    }
}

fun localClassWithNestedClass() {
    class Local {
        class NestedInsideLocal
        private (x: Int)
    }
}
