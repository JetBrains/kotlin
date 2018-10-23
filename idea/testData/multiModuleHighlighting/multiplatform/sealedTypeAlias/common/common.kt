package p

expect sealed class Presence {
    object Online: Presence
    object Offline: Presence
}