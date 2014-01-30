// "class org.jetbrains.jet.plugin.quickfix.MoveWhenElseBranchFix" "false"
// ERROR: 'else' entry must be the last one in a when-expression
// ERROR: 'else' entry must be the last one in a when-expression
// WARNING: Unreachable code
// WARNING: Unreachable code
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