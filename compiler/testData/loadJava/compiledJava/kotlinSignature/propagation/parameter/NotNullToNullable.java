package test;

import org.jetbrains.annotations.NotNull;

public interface NotNullToNullable {

    public interface Super {
        void foo(@NotNull String p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(String p);
    }
}
