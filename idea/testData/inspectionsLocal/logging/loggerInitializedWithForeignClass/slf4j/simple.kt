// WITH_RUNTIME
package org.slf4j

class Foo {
    private val logger = LoggerFactory.getLogger(<caret>Bar::class.java)
}

class Bar

object LoggerFactory {
    fun getLogger(clazz: Class<*>) {}
}