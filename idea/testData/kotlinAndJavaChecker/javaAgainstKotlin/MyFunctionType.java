package test;

public class MyFunctionType {
    public void foo() {
        MyFunction<CharSequence, Integer> function1 = (CharSequence x) -> x.length();
        bar(x -> x.length());
        bar(CharSequence::length);
    }

    public void bar(MyFunction<CharSequence, Integer> x) {}
}
