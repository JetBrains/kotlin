// "Make 'ordinal' in E and X open" "false"
// ERROR: This type is final, so it cannot be inherited from
// ERROR: Cannot access '<init>': it is 'private' in 'E'
// ERROR: 'ordinal' in 'E' is final and cannot be overridden
// ACTION: Convert to extension
// ACTION: Disable 'Convert to extension'
// ACTION: Edit intention settings
enum class E {}
trait X {
    final fun ordinal() : Int = 42
}

class A : E(), X {
    override<caret> fun ordinal() : Int = 24;
}
