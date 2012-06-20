package test;

import java.lang.String;
import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.BitSet;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class VarargReplacedWithNotVararg {
    @KotlinSignature("fun foo(s : String)")
    public void foo(String... s) {
    }
}
