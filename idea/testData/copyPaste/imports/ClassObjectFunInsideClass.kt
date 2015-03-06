package a

// ALLOW_UNRESOLVED
//NOTE: is is an unsupported case
class A {
    <selection>fun g() {
        f()
    }</selection>

    default object {
        fun f()
    }
}