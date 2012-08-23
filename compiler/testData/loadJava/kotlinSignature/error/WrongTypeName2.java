package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class WrongTypeName2 {
    @KotlinSignature("fun foo(a : Something.String) : String")
    public String foo(String a) {
        throw new UnsupportedOperationException();
    }
}
