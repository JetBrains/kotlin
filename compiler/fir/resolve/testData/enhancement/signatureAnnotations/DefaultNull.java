// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {
    public void foo(@DefaultNull Integer x) {}
    public void bar(@DefaultNull int x) {}
}

// FILE: B.java
import kotlin.annotations.jvm.internal.*;

public class B<T> {
    public void foo(@DefaultNull T t) { }
}

