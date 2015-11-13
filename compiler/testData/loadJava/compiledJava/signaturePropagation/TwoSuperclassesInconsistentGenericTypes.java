package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface TwoSuperclassesInconsistentGenericTypes {
    List<String> foo();

    void dummy(); // To make it not SAM

    public interface Other {
        List<String> foo();

        void dummy(); // To make it not SAM
    }

    public class Sub implements TwoSuperclassesInconsistentGenericTypes, Other {
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }

        public void dummy() {}
    }
}
