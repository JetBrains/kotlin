// LANGUAGE: -SoundSmartcastForEnumEntries
// DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

enum class Message(val text: String?) {
    HELLO("hello"),
    WORLD("world"),
    NOTHING(null)
}

fun printMessages() {
    Message.HELLO.text!!
    Message.HELLO.text.length

    Message.NOTHING.text<!UNSAFE_CALL!>.<!>length

    Message.NOTHING.text!!
    Message.NOTHING.text.length
}
