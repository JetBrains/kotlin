package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface TwoSuperclassesInconsistentGenericTypes {
    @KotlinSignature("fun foo(): MutableList<String?>")
    List<String> foo();

    public interface Other {
        @KotlinSignature("fun foo(): MutableList<String>?")
        List<String> foo();
    }

    public class Sub implements TwoSuperclassesInconsistentGenericTypes, Other {
        //@ExpectLoadError("Incompatible types in superclasses: [String?, String]")
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }
    }
}