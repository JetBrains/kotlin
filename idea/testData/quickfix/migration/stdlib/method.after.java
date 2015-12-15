// "Replace with new qualified name" "true"
// WITH_RUNTIME

import kotlin.jvm.JvmClassMappingKt;

class C {
    public void foo(Class cls) {
        <caret>JvmClassMappingKt.getKotlinClass(cls);
    }
}