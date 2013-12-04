// !CALL: invoke
// !EXPLICIT_RECEIVER_KIND: BOTH_RECEIVERS
// !THIS_OBJECT: f
// !RECEIVER_ARGUMENT: 1

fun bar(f: Int.()->Unit) {
    1.f()
}
