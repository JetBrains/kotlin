package test;

import java.lang.String;
import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.BitSet;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class MethodWithVararg {
    @KotlinSignature("fun foo(vararg s : String)")
    public void foo(String... s) {
    }
}
