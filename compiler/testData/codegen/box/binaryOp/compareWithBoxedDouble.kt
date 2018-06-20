// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE
// reason - multifile tests are not supported in JS tests
//FILE: Holder.java

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
