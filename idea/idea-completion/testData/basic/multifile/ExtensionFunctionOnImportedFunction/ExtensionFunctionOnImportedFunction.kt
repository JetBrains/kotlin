// FIR_COMPARISON
package first

// For KT-3102

import second.foo

fun test() = foo().ext<caret>

// EXIST: extensionFunction1, extensionFunction2