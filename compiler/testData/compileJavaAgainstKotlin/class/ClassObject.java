class ClassObject {
    void accessToClassObject() {
        WithClassObject.object.$instance.foo();
        WithClassObject.object.$instance.getValue();
        WithClassObject.object.$instance.getValueWithGetter();
        WithClassObject.object.$instance.getVariable();
        WithClassObject.object.$instance.setVariable(0);
        WithClassObject.object.$instance.getVariableWithAccessors();
        WithClassObject.object.$instance.setVariableWithAccessors(0);
    }

    void accessToPackageObject() {
        PackageInner.$instance.foo();
        PackageInner.$instance.getValue();
    }

    void accessToInnerClass() {
        new WithClassObject.MyInner().foo();
        new WithClassObject.MyInner().getValue();
    }
}