// WITH_RUNTIME
// PROBLEM: Logger initialized with foreign class 'Bar::class'
// FIX: Replace with 'Foo::class'
package org.apache.commons.logging

class Foo {
    private val logger = LogFactory.getLog(<caret>Bar::class.java)
}

class Bar

object LogFactory {
    fun getLog(clazz: Class<*>) {}
    fun getLog(name: String?) {}
}