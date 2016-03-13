// "Implement interface" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

class Container {
    interface <caret>Base {
        val x: Boolean

        val y: Double
            get() = 3.14

        fun foo(): String = ""

        fun bar(z: Int): String
    }
}

