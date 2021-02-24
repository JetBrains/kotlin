// FILE: Annotated.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Annotated {
    @NotNull
    public String foo(@Nullable String param) {
        if (param != null) return param;
        else return "";
    }
}

// FILE: AnnotatedDerived.java

public class AnnotatedDerived extends Annotated {
    public String foo(String param) {
        return super.foo(param);
    }
}

// FILE: jvm.kt

class User : AnnotatedDerived() {
    fun test() {
        val x = foo("123")
        val y = foo(null)
    }
}
