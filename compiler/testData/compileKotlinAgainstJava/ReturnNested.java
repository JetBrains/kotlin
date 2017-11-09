package test;

public class ReturnNested {

    public static class Nested {
        
    }

    public Nested getNested() { return new Nested(); }

}
