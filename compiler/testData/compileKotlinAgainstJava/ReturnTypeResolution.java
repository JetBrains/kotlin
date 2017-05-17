package test;

public class ReturnTypeResolution {

     public class SomeClass {}

     public SomeClass getSomeClass() { return new SomeClass(); }

}

class SomeClass {

}
