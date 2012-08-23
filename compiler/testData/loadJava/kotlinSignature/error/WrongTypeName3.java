package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class WrongTypeName3 {
    @KotlinSignature("fun foo(a : String) : List")
    public String foo(String a) {
        throw new UnsupportedOperationException();
    }
}
