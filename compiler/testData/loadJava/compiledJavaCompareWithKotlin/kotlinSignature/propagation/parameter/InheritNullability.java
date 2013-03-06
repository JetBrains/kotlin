package test;

import org.jetbrains.annotations.NotNull;

public interface InheritNullability {

    public interface Super {
        void foo(@NotNull String p);
    }

    public interface Sub extends Super {
        void foo(String p);
    }
}