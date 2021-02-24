// RUNTIME_WITH_FULL_JDK
package util

import java.util.logging.Logger

class Foo {
    private val logger = Logger.getLogger(<caret>Bar::class.qualifiedName)
}

class Bar