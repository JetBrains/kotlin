package test;

class ReturnInnerClass extends ReturnInnerClassImpl {

}

class ReturnInnerClassImpl extends AbstractReturnInnerClass {

}

abstract class AbstractReturnInnerClass {

    class InnerClass {}

    InnerClass getInnerClass() { return null; }

}
