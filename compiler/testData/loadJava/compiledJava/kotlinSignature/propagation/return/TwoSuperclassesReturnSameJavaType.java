package test;

import org.jetbrains.annotations.NotNull;

public interface TwoSuperclassesReturnSameJavaType {

    public interface Super1 {
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2 {
        @NotNull
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1, Super2 {
        CharSequence foo();
    }
}
