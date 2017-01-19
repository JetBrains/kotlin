// IGNORE_BACKEND: JS
// reason - multifile tests are not supported in JS tests
// IGNORE_BACKEND: NATIVE
// reason - no java interop. Consider testing by another way

//FILE: Holder.kt

class Holder {
    public Double value;
    public Holder(Double value) { this.value = value; }
}

//FILE: test.kt

import Holder

fun box(): String {
    val j = Holder(0.99)
    return if (j.value > 0) "OK" else "fail"
}