package test;

import java.util.List;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface ArraysInSubtypes {
    interface Super {
        CharSequence[] array();
        List<? extends CharSequence[]> listOfArray();

        Object[] objArray();
    }

    interface Sub<T> extends Super {
        String[] array();

        List<? extends String[]> listOfArray();

        T[] objArray();
    }
}