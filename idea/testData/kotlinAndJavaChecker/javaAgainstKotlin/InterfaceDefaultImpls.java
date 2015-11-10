import lib.*;

class C {
    public static void test(NonAbstractFun i, NonAbstractFunWithExpressionBody i2, NonAbstractProperty i3, NonAbstractPropertyWithBody i4) {
        AllAbstract.<error descr="Cannot resolve symbol 'DefaultImpls'">DefaultImpls</error>;

        NonAbstractFun.DefaultImpls.f(i);
        NonAbstractFunWithExpressionBody.DefaultImpls.f(i2);

        NonAbstractProperty.DefaultImpls.getC(i3);
        NonAbstractPropertyWithBody.DefaultImpls.getC(i4);
    }
}
