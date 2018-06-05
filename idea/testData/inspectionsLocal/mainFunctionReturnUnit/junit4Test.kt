// PROBLEM: JUnit test should return Unit
// FIX: Add explicit Unit return type
// DISABLE-ERRORS
package org.junit

annotation class Test

class A {
    @Test fun foo<caret>() = 1
}