// WITH_RUNTIME
package org.apache.commons.logging

class Foo {
    private val logger = LogFactory.getLog(<caret>Bar::class.simpleName)
}

class Bar

object LogFactory {
    fun getLog(clazz: Class<*>) {}
    fun getLog(name: String?) {}
}