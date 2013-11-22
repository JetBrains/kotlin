// "Move else branch to the end" "false"
// ERROR: 'else' entry must be the last one in a when-expression
// ERROR: 'else' entry must be the last one in a when-expression
// WARNING: Unreachable code
// WARNING: Unreachable code
// ACTION: Disable 'Eliminate Argument of 'when''
// ACTION: Disable 'Replace 'when' with 'if''
// ACTION: Edit intention settings
// ACTION: Edit intention settings
// ACTION: Eliminate argument of 'when'
// ACTION: Replace 'when' with 'if'
package foo

fun foo() {
   var i = 2
   when (i) {
       1, 2 -> { /* some code */ }
       el<caret>se -> { /* first else branch */ }
       else -> { /* second else branch */ }
       3 -> { /* some other code */ }
   }
}