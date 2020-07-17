// WITH_RUNTIME
package org.apache.log4j

class Foo {
    private val logger = Logger.getLogger(<caret>Bar::class.java)
}

class Bar

object Logger {
    fun getLogger(clazz: Class<*>) {}
}