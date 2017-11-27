// WITH_RUNTIME

import kotlin.test.assertEquals

interface Notification

data class Email(val sender: String, val title: String, val body: String) : Notification

data class SMS(val caller: String, val message: String) : Notification

data class VoiceRecording(val contactName: String, val link: String) : Notification

fun showNotification(notification: Notification): String = when(notification) {
    match Email(email, title, _) -> "You got an email from $email with title: $title"
    match SMS(number, message) -> "You got an SMS from $number! Message: $message"
    match VoiceRecording(name, link) -> "You received a Voice Recording from $name! Click the link to hear it: $link"
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun box() : String {
    val someSms = SMS("12345", "Are you there?")
    val someVoiceRecording = VoiceRecording("Tom", "voicerecording.org/id/123")
    assertEquals(showNotification(someSms), "You got an SMS from 12345! Message: Are you there?")
    assertEquals(showNotification(someVoiceRecording), "You received a Voice Recording from Tom! Click the link to hear it: voicerecording.org/id/123")
    return "OK"
}
