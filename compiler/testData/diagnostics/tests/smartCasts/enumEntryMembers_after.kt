// !LANGUAGE: +SoundSmartcastForEnumEntries
// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

enum class Message(val text: String?) {
    HELLO("hello"),
    WORLD("world"),
    NOTHING(null)
}

fun printMessages() {
    Message.HELLO.text!!
    <!DEBUG_INFO_SMARTCAST!>Message.HELLO.text<!>.length

    Message.NOTHING.text<!UNSAFE_CALL!>.<!>length

    Message.NOTHING.text!!
    <!DEBUG_INFO_SMARTCAST!>Message.NOTHING.text<!>.length
}
