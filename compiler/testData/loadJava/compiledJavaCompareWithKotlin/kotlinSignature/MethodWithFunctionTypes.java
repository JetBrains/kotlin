package test;

import java.lang.String;
import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import jet.*;

public class MethodWithFunctionTypes {
    @KotlinSignature("fun foo(f : (String?) -> String) : (String.() -> String?)?")
    public ExtensionFunction0<String, String> foo(Function1<String, String> f) {
        throw new UnsupportedOperationException();
    }
}
