// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

class A {
    public void first(@DefaultValue("hello") String value) {
    }

    public void second(@DefaultValue("first") String a, @DefaultValue("second") String b) {
    }

    public void third(@DefaultValue("first") String a, String b) {
    }

    public void fourth(String first, @DefaultValue("second") String second) {
    }

    public void wrong(@DefaultValue("hello") Integer i) {
    }
}


