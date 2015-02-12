// SHOULD_FAIL_WITH: Can't replace foreign reference with call expression: test.TestPackage.bar
package test

val <caret>bar: Int
    get() = 1