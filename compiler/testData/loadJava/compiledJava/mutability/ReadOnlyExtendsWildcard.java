// JVM_ANNOTATIONS
package test;

import kotlin.annotations.jvm.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface ReadOnlyExtendsWildcard {
    void bar(); // Non-SAM
    void foo(@ReadOnly List<? extends CharSequence> x, @NotNull Comparable<? super String> y);
}
