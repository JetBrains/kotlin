// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

class TestClass {
    // 1. Properties
    public val publicProperty = "keep me"
    private val unusedPrivateProperty = "delete me"

    // 2. Backing Fields
    public var publicBackingField = "keep me"
    private var unusedPrivateBackingField = "delete me"

    // 3. Type Aliases
    public typealias PublicAlias = String
    private typealias UnusedPrivateAlias = String

    // 4. Functions
    public fun publicFunction() {}
    private fun unusedPrivateFunction() {}

    // 5. Inner Classes
    public class PublicInnerClass {}
    private class UnusedPrivateInnerClass {}

    // 6. Companion Objects
    public companion object PublicCompanion {}
    private companion object UnusedCompanion {}

    // 7. Inline Usage
    private val usedPropertyByInline = "keep me too"
    inline fun witnessForProperty() {
        val x = usedPropertyByInline
    }

    private var usedBackingFieldByInline = 0
    inline fun witnessForField() {
        usedBackingFieldByInline = 42
    }

    private fun usedByInline() {}
    inline fun witnessForFunction() {
        usedByInline()
    }

    private companion object UsedByInlineCompanion {
        fun companionFun() {}
    }
    inline fun witnessForCompanion() {
        UsedByInlineCompanion.companionFun()
    }

    // 8. API Usage
    // Type Alias used in Public API Return Type
    private typealias KeptPrivateAlias = String
    public val aliasUser: KeptPrivateAlias = "fast"

    // Class used in Return Type
    private class KeptPrivateInner {}
    public fun provideInner(): KeptPrivateInner = KeptPrivateInner()

    // 9. Partial Pruning
    private class MixedClass {
        fun used() {}
        fun unused() {}
    }
    private companion object MixedCompanion {
        fun used() {}
        fun unused() {}
    }
    public inline fun useMixed() {
        MixedClass().used()
        MixedCompanion.used()
    }

    private class NestedMixedOuter {
        class Nested {
            fun used() {}
            fun unused() {}
        }
    }
    public inline fun useNestedMixed() { NestedMixedOuter.Nested().used() }

    // 11. Chained Calls Verification
    private class InlineChain {
        fun used() {}
    }
    private inline fun privateInlineChainMiddle() {
        InlineChain().used()
    }
    public inline fun publicInlineChainStart() {
        privateInlineChainMiddle()
    }

    private class BrokenChainEnd {
        fun unused() {}
    }
    private fun privateNonInlineMiddle() {
        BrokenChainEnd().unused()
    }
    public inline fun publicBrokenChainStart() {
        privateNonInlineMiddle()
    }
}

// 9. Top-level Check
public fun topLevelKept() {}
private fun topLevelUnused() {}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, companionObject, functionDeclaration, inline, integerLiteral,
localProperty, nestedClass, objectDeclaration, propertyDeclaration, typeAliasDeclaration */
