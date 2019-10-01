// "Implements 'getName'" "false"
// DISABLE-ERRORS
// ACTION: Convert to secondary constructor
// ACTION: Create test
// ACTION: Make private
// ACTION: Make protected
// ACTION: Make public
// ACTION: Move to class body
class A(<caret>internal val name: String) : JavaInterface {
}