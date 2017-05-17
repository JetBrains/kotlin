package test;

public class TypeArgumentInSuperType {


    public static final class Impl implements Interface2.InnerInterface<String> {

    }


}

interface Interface1 {

    interface InnerInterface<T> {

    }

}

interface Interface2 extends Interface1 {

}
