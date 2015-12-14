// "Replace with new qualified name" "true"
// WITH_RUNTIME

import kotlin.collections.ArraysKt;

class C {
    public void foo(byte[] bytes) {
        ArraysKt.component1(bytes);
    }
}