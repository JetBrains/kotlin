package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

import java.util.*;

public interface TwoSuperclassesConflictingProjectionKinds {

    public interface Super1 {
        @KotlinSignature("fun foo(): MutableCollection<CharSequence>")
        public Collection<CharSequence> foo();
    }

    public interface Super2 {
        @KotlinSignature("fun foo(): MutableCollection<out CharSequence>")
        public Collection<CharSequence> foo();
    }

    public interface Sub extends Super1, Super2 {
        public Collection<CharSequence> foo();
    }
}
