package test;

import org.jetbrains.annotations.NotNull;

public interface InheritNullabilityJavaSubtype {

    public interface Super {
        @NotNull
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        String foo();
    }
}
