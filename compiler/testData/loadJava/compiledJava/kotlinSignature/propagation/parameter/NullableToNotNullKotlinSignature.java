package test;

import org.jetbrains.annotations.NotNull;

public interface NullableToNotNullKotlinSignature {

    public interface Super {
        void foo(String p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(String p);
    }
}
