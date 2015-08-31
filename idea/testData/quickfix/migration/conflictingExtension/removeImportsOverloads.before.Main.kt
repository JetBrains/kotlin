// "Delete redundant extension property" "true"
package utils

import java.io.File

val File.<caret>name: String
    get() = getName()

val Thread.name: String
    get() = getName()

// WITH_RUNTIME