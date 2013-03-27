package test;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritNotVarargNotNull {

    public interface Super {
        @KotlinSignature("fun foo(p: Array<out String>)")
        void foo(String[] p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(String... p);
    }
}
