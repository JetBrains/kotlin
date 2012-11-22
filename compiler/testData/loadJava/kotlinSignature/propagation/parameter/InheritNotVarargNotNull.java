package test;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritNotVarargNotNull {

    public interface Super {
        @KotlinSignature("fun foo(p: Array<out String>)")
        void foo(String[] p);
    }

    public interface Sub extends Super {
        void foo(String... p);
    }
}
