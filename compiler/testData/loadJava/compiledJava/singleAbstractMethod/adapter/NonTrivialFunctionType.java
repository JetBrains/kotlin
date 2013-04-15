package test;

import java.io.FilenameFilter;
import java.util.Comparator;

public class NonTrivialFunctionType {
    public void foo(FilenameFilter filenameFilter) {
    }

    public void foo(Comparator<String> comparator) {
    }

    public void wildcardUnbound(Comparator<?> comparator) {
    }

    public void wildcardBound(Comparator<? super CharSequence> comparator) {
    }
}