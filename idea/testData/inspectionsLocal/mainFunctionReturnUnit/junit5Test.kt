// FIX: Add explicit Unit return type
// DISABLE-ERRORS
package org.junit.jupiter.api

annotation class Test

class A {
    @Test fun foo<caret>() = 1
}