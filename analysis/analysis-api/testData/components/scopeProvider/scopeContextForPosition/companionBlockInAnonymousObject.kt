// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Receiver {
    val receiverProperty: Int = 0
}

fun Receiver.test() {
    val o = object {
        val memberProperty: Int = 1

        companion {
            fun foo() {
                <expr>receiverProperty</expr>
            }
        }
    }
}
