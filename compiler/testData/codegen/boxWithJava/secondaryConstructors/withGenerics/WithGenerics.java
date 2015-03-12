class WithGenerics {
    public static String foo1() {
        A<Double> x = new A<Double>("OK");
        return x.toString();
    }

    public static String foo2() {
        A<Integer> x = new A<Integer>(123);
        return x.toString();
    }
}
