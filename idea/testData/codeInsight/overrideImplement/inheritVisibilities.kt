open class A() {
    protected open fun protectedFun() { }
    internal open fun internalFun() {}
    public open fun publicFun() {}
}

interface B {
   protected open val protectedProperty : Int
   internal open val internalProperty : Int
   public open val publicProperty : Int
}

class C : A(), B {
   <caret>
}
