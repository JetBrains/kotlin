// TARGET_PLATFORM: JS
// WITH_STDLIB

// Issue: KTIJ-28546
// IGNORE_FE10

fun test() {
    "some".asDy<caret>namic()
}
