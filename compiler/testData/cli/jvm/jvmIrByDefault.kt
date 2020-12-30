// This test checks that with -language-version 1.4 we're using the old JVM backend,
// and with -language-version 1.5 -- the new JVM IR backend.
// JVM IR doesn't produce classes for local functions, so the test checks which backend
// is used by asserting that the file for the anonymous class does or doesn't exist.

// Feel free to remove both _1_4 and _1_5 tests as soon as either the old JVM backend
// or LV 1.4 is removed, whichever happens earlier.

class C {
    fun test() {
        fun local() {}
    }
}
