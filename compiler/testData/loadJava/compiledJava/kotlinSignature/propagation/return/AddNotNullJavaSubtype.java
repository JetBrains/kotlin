package test;

import org.jetbrains.annotations.NotNull;

public interface AddNotNullJavaSubtype {

    public interface Super {
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        @NotNull
        String foo();
    }
}
