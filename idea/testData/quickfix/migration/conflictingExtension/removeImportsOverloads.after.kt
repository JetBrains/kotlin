// "Delete redundant extension property" "true"
package utils

import java.io.File

val Thread.name: String
    get() = getName()

// WITH_RUNTIME