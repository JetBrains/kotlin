package test;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritVarargNotNull {

    public interface Super {
        @KotlinSignature("fun foo(vararg p: String)")
        void foo(String... p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(String[] p);
    }
}
