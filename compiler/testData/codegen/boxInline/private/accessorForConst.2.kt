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


