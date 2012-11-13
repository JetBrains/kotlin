package test;

import org.jetbrains.annotations.NotNull;

public interface AddNotNullJavaSubtype {

    public interface Super {
        CharSequence foo();
    }

    public interface Sub extends Super {
        @NotNull
        String foo();
    }
}