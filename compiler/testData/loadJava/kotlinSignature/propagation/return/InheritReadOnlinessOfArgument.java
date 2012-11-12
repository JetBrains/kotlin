package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritReadOnlinessOfArgument {
    @KotlinSignature("fun foo(): List<List<String>>>")
    public List<List<String>> foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritReadOnlinessOfArgument {
        public List<List<String>> foo() {
            throw new UnsupportedOperationException();
        }
    }
}
