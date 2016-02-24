// FILE: 1.kt

package test

class IntentionsBundle {
    companion object {
        internal inline fun message(): String {
            return KEY + BUNDLE
        }

        private const val BUNDLE = "K"
        protected const val KEY = "O"
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return IntentionsBundle.message()
}
