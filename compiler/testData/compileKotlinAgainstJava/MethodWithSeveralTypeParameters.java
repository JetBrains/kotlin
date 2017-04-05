package test;

class MethodWithSeveralTypeParameters {

    public static <T extends CharSequence, N extends Number> N  method(T str, N number) { return null; }

}
