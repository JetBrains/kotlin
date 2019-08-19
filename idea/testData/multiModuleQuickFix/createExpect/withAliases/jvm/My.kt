// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

import kotlin.Double as MyDouble

typealias MyInt = Int

actual class <caret>My {
    actual fun foo(): MyInt = 42

    actual val some: MyDouble = 4.0
}
