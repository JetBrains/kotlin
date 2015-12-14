// "Replace with new qualified name" "true"
// WITH_RUNTIME

import kotlin.ArraysKt;

class C {
    public void foo(byte[] bytes) {
        <caret>ArraysKt.component1(bytes);
    }
}