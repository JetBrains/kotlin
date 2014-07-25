package test;

class ClassObject {
    void accessToClassObject() {
        WithClassObject.OBJECT$.foo();
        WithClassObject.OBJECT$.getValue();
        WithClassObject.OBJECT$.getValueWithGetter();
        WithClassObject.OBJECT$.getVariable();
        WithClassObject.OBJECT$.setVariable(0);
        WithClassObject.OBJECT$.getVariableWithAccessors();
        WithClassObject.OBJECT$.setVariableWithAccessors(0);
    }

    void accessToPackageObject() {
        PackageInner.INSTANCE$.foo();
        PackageInner.INSTANCE$.getValue();
    }

    void accessToInnerClass() {
        new WithClassObject.MyInner().foo();
        new WithClassObject.MyInner().getValue();
    }
}
