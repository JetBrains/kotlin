package test;

import java.util.List;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface ReturnNotSubtype {
    interface Super<T> {
        T t();

        void _void();

        String string1();
        String string2();

        Class<? extends CharSequence> klass();

        T[] array();
    }

    interface Sub extends Super<Boolean> {
        Void t();

        boolean _void();

        void string1();
        List<Boolean> string2();

        Class<?> klass();

        Void[] array();
    }
}