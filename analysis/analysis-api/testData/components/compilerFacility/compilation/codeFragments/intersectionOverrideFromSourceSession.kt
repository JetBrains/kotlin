// ISSUE: KT-69951
// MODULE: context

// FILE: Base.java
public interface Base {
    String getParent();
}

// FILE: Derived.java
public interface Derived extends Base {
    @Override
    String getParent();
}

// FILE: Impl.java
public class Impl implements Base {
    @Override
    public String getParent() {
        return "hello";
    }
}


// FILE: KotlinImpl.kt
class KotlinImpl : Impl(), Derived {
    init {
        <caret_context>Unit
    }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
parent