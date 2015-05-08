// "Move else branch to the end" "false"
// ERROR: 'else' entry must be the last one in a when-expression
// ERROR: 'else' entry must be the last one in a when-expression
// ERROR: Unreachable code
// ERROR: Unreachable code
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