package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;

public class CustomProjectionKind {
    @KotlinSignature("fun foo(): MutableList<out Number>")
    public List<Number> foo() {
        throw new UnsupportedOperationException();
    }
}
