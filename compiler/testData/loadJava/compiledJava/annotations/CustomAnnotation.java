package test;

public interface CustomAnnotation {

    @MyAnnotation(MyEnum.ONE)
    public class MyTest {}

    public @interface MyAnnotation {
        MyEnum value();
    }

    public enum MyEnum {
        ONE
    }
}
