package test;

import org.jetbrains.annotations.NotNull;

public interface AddNotNullSameJavaType {

    public interface Super {
        CharSequence foo();
    }

    public interface Sub extends Super {
        @NotNull
        CharSequence foo();
    }
}