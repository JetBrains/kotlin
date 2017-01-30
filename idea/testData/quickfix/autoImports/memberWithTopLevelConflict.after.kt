import p1.sleep

// "Import" "true"
// WITH_RUNTIME
// FULL_JDK
// ERROR: Unresolved reference: sleep


fun usage() {
    sleep<caret>()
}