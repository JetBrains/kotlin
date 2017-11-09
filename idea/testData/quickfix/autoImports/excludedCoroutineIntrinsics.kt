// "Import" "false"
// WITH_RUNTIME
// ACTION: Create function 'suspendCoroutineOrReturn'
// ACTION: Rename reference
// ERROR: Unresolved reference: suspendCoroutineOrReturn

fun some() {
    suspendCoroutineOrReturn<caret> {}
}
