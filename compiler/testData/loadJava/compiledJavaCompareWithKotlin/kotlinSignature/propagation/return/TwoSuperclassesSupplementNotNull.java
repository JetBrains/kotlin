package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import java.util.*;
import jet.*;

public interface TwoSuperclassesSupplementNotNull {

    public interface Super1 {
        @KotlinSignature("fun foo(): List<String?>")
        public List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2 {
        @KotlinSignature("fun foo(): List<String>?")
        public List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1, Super2 {
        public List<String> foo();
    }
}
