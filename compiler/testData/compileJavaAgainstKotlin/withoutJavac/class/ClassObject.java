package test;

class ClassObject {
    void accessToClassObject() {
        WithClassObject.Companion.foo();
        WithClassObject.Companion.getValue();
        WithClassObject.Companion.getValueWithGetter();
        WithClassObject.Companion.getVariable();
        WithClassObject.Companion.setVariable(0);
        WithClassObject.Companion.getVariableWithAccessors();
        WithClassObject.Companion.setVariableWithAccessors(0);
    }

    void accessToPackageObject() {
        PackageInner.INSTANCE.foo();
        PackageInner.INSTANCE.getValue();
    }

    void accessToInnerClass() {
        new WithClassObject.MyInner().foo();
        new WithClassObject.MyInner().getValue();
    }
}
