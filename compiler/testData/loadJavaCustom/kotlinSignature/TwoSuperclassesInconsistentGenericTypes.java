package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public interface TwoSuperclassesInconsistentGenericTypes {
    @KotlinSignature("fun foo(): MutableList<String?>")
    List<String> foo();

    public interface Other {
        @KotlinSignature("fun foo(): MutableList<String>?")
        List<String> foo();
    }

    public class Sub implements TwoSuperclassesInconsistentGenericTypes, Other {
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }
    }
}