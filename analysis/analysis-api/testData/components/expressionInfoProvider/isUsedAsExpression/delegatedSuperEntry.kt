interface I
class Delegating : I by <expr>compoundDelegate()</expr>
fun compoundDelegate(): I {
    return object : I {}
}