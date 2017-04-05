package test;

class ReturnInnerInInner {

    static class Inner {
        Inner getInner() { return null; }

        ReturnInnerInInner.Inner getInner2() { return null; }

        test.ReturnInnerInInner.Inner getInner3() { return null; }
    }

}
