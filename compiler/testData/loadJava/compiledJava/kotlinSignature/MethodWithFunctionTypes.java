package test;

import java.lang.String;
import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import kotlin.jvm.functions.*;

public class MethodWithFunctionTypes {
    @KotlinSignature("fun foo(f : (String?) -> String) : (() -> String?)?")
    public Function0<String> foo(Function1<String, String> f) {
        throw new UnsupportedOperationException();
    }
}
