// SKIP_TXT
// FIR_DUMP

abstract class InlineCompletionSessionManager {
    protected class Proto {
        class Some
    }
}

fun checkCannotAccess() {
    object : InlineCompletionSessionManager() {
        fun chch() {
            val b: Proto = Proto()
            if (b is <!INCOMPATIBLE_TYPES!>Proto.Some<!>) return
        }
    }
}
