import testData.libraries.*;

class TestOverload {
    void foo() {
        OverloadedFunKt.overloadedFun("", null, 2, null);

        OverloadedFunKt.overloadedFun("", null, true, 2, null);

        OverloadedFunKt.overloadedFun("", null, true, 2, 3, null);
    }
}