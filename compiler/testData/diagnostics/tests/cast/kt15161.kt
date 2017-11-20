// !WITH_NEW_INFERENCE
class Array<E>(e: E) {
    val k = Array(1) {
        1 <!USELESS_CAST!>as Any<!>
        e as Any?
    }
}