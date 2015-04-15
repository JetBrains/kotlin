package test;

public class ClassObjectInParamVariance {
    public @interface Anno {
        Class<? extends Integer> arg1();
        Class<? super Integer> arg2();

        Class<? extends Integer>[] arg3();
        Class<? super Integer>[] arg4();

        Class<? extends Class<?>>[] arg5();
        Class<? super Class<?>>[] arg6();

        Class<? extends Class<Integer>>[] arg7();
        Class<? super Class<Integer>>[] arg8();
    }
}
