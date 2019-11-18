// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
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
