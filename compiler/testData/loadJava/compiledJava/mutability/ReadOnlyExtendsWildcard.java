package test;

import org.jetbrains.annotations.*;

import java.util.*;

public interface ReadOnlyExtendsWildcard {
    void bar(); // Non-SAM
    void foo(@ReadOnly List<? extends CharSequence> x, @NotNull Comparable<? super String> y);
}
