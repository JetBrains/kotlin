// "Make E open" "false"
// ERROR: This type is final, so it cannot be inherited from
// ERROR: Cannot access '<init>': it is 'private' in 'E'
enum class E {}
class A : E<caret>() {}
