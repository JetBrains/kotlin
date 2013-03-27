package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import java.util.*;

public interface TwoSuperclassesInvariantAndCovariantInferMutability {

    public interface Super1 {
        @KotlinSignature("fun foo(): List<List<String>>")
        public List<List<String>> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2 {
        @KotlinSignature("fun foo(): MutableList<MutableList<String>>")
        public List<List<String>> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1, Super2 {
        public List<List<String>> foo();
    }
}
