package test;

import java.lang.Number;
import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class WrongTypeVariance {
    @KotlinSignature("fun copy(a : List<out Number>, b : List<Number>) : List<Number>")
    public List<Number> copy(List<? extends Number> from, List<? super Number> to) {
        throw new UnsupportedOperationException();
    }
}
