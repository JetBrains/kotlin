package test;

import org.jetbrains.annotations.NotNull;

import java.lang.String;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface TwoSuperclassesVarargAndNot {
    public interface Super1 {
        void foo(String... s);
    }

    public interface Super2 {
        @KotlinSignature("fun foo(s : Array<out String?>?)")
        void foo(String[] s);
    }

    public interface Sub extends Super1, Super2 {
        @ExpectLoadError(//"Incompatible projection kinds in type arguments of super methods' return types: [String?, out String?]|" +
                         //"Incompatible types in superclasses: [Array<String?>, Array<out String?>?]|" +
                         "Incompatible super methods: some have vararg parameter, some have not|")
        void foo(String[] s);
    }
}