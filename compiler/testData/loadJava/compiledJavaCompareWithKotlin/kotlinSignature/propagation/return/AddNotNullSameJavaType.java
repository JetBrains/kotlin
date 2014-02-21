package test;

import org.jetbrains.annotations.NotNull;

public interface AddNotNullSameJavaType {

    public interface Super {
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        @NotNull
        CharSequence foo();
    }
}
