package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class MethodWithGenerics {
    @KotlinSignature("fun foo(a : String, b : List<Entry<String?, String>?>) : String")
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
