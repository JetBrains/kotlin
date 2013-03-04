class ClassObject {
    void accessToClassObject() {
        WithClassObject.object$.foo();
        WithClassObject.object$.getValue();
        WithClassObject.object$.getValueWithGetter();
        WithClassObject.object$.getVariable();
        WithClassObject.object$.setVariable(0);
        WithClassObject.object$.getVariableWithAccessors();
        WithClassObject.object$.setVariableWithAccessors(0);
    }

    void accessToPackageObject() {
        PackageInner.instance$.foo();
        PackageInner.instance$.getValue();
    }

    void accessToInnerClass() {
        new WithClassObject.MyInner().foo();
        new WithClassObject.MyInner().getValue();
    }
}