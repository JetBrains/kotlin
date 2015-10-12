// "class org.jetbrains.kotlin.idea.quickfix.MakeOverriddenMemberOpenFix" "false"
// ERROR: This type is final, so it cannot be inherited from
// ERROR: Cannot access '<init>': it is 'private' in 'E'
// ERROR: 'ordinal' in 'E' is final and cannot be overridden
enum class E {}
interface X {
    final val ordinal : Int
            get() = 42
}

class A : E(), X {
    override<caret> val ordinal : Int = 24;
}
