// "Create actual class for module testModule_JVM (JVM)" "true"
// DISABLE-ERRORS

package my

import other.Another
import other.Other

expect class My<caret> : Other<Another>