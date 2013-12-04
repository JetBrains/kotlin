// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: THIS_OBJECT
// !THIS_OBJECT: f
// !RECEIVER_ARGUMENT: NO_RECEIVER

fun bar(f: ()->Unit) {
    f()
}
