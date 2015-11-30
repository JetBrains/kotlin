// WITH_RUNTIME
fun x(items : Sequence<String>) {}
fun y() {
    x(<selection>foo@ (sequenceOf(""))</selection>)
}
/*
items
of
sequence
sequenceOf
*/