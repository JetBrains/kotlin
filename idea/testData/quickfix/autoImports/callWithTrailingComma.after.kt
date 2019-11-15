// "Import" "true"
// ERROR: Type mismatch: inferred type is Int but String was expected
// ACTION: Add 'toString()' call
// ACTION: Change parameter 'p' type of function 'foo' to 'Int'
// ACTION: Create function 'foo'
// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas

package main

import other.foo

class X {
    fun foo(p: String) {
    }

    fun f(p: Int) {
        foo(<selection><caret></selection>p, )
    }
}
