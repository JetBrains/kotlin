// "Replace with new qualified name" "true"
// WITH_RUNTIME

import kotlin.jvm.ClassMapping;

class C {
    public void foo(Class cls) {
        <caret>ClassMapping.getKotlin(cls);
    }
}