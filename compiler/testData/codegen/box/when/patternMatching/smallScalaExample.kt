// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Notification

data class Email(val sender: String, val title: String, val body: String) : Notification()

data class SMS(val caller: String, val message: String) : Notification()

data class VoiceRecording(val contactName: String, val link: String) : Notification()

fun showNotification(notification: Notification): String = when(notification) {
    is Email(val email, val title, _) -> "You got an email from $email with title: $title"
    is SMS(val number, val message) -> "You got an SMS from $number! Message: $message"
    is VoiceRecording(val name, val link) -> "You received a Voice Recording from $name! Click the link to hear it: $link"
}

fun showImportantNotification(notification: Notification, importantPeopleInfo: Sequence<String>): String = when (notification) {
    is Email(val email, _, _) && importantPeopleInfo.contains(email) -> "You got an email from special someone!"
    is SMS(val number, _) && importantPeopleInfo.contains(number) -> "You got an SMS from special someone!"
    is val other -> showNotification(other)
}

fun box() : String {
    val importantPeopleInfo = sequenceOf("867-5309", "jenny@gmail.com")

    val someSms = SMS("867-5309", "Are you there?")
    val someVoiceRecording = VoiceRecording("Tom", "voicerecording.org/id/123")
    val importantEmail = Email("jenny@gmail.com", "Drinks tonight?", "I'm free after 5!")
    val importantSms = SMS("867-5309", "I'm here! Where are you?")

    assertEquals(showImportantNotification(someSms, importantPeopleInfo), "You got an SMS from special someone!")
    assertEquals(showImportantNotification(someVoiceRecording, importantPeopleInfo), "You received a Voice Recording from Tom! Click the link to hear it: voicerecording.org/id/123")
    assertEquals(showImportantNotification(importantEmail, importantPeopleInfo), "You got an email from special someone!")
    assertEquals(showImportantNotification(importantSms, importantPeopleInfo), "You got an SMS from special someone!")

    return "OK"
}
