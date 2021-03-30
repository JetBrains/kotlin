// FIR_COMPARISON
package first

// For KT-3096 No completion in function literal

import second.someWithLiteral

fun test() {
  someWithLiteral({file -> file.testFu<caret>})
}

// EXIST: testFunction1, testFunction2