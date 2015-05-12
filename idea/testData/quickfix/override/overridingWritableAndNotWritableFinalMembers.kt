// "class org.jetbrains.kotlin.idea.quickfix.MakeOverriddenMemberOpenFix" "false"
// ERROR: This type is final, so it cannot be inherited from
// ERROR: Cannot access '<init>': it is 'private' in 'E'
// ERROR: 'ordinal' in 'E' is final and cannot be overridden
enum class E {}
interface X {
    final fun ordinal() : Int = 42
}

class A : E(), X {
    override<caret> fun ordinal() : Int = 24;
}
