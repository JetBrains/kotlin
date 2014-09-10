package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

import java.util.*;

public interface TwoSuperclassesConflictingProjectionKinds {

    public interface Super1 {
        @KotlinSignature("fun foo(): MutableCollection<CharSequence>")
        public Collection<CharSequence> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2 {
        @KotlinSignature("fun foo(): MutableCollection<out CharSequence>")
        public Collection<CharSequence> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1, Super2 {
        //@ExpectLoadError("Incompatible projection kinds in type arguments of super methods' return types: [CharSequence, out CharSequence]")
        public Collection<CharSequence> foo();
    }
}
