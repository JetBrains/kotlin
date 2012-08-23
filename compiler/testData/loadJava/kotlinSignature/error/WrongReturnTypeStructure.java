package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class WrongReturnTypeStructure {
    @KotlinSignature("fun foo(a : String, b : List<Map.Entry<String?, String>?>) : String<Int>?")
    public String foo(String a, List<Map.Entry<String, String>> b) {
        throw new UnsupportedOperationException();
    }
}
