class ClassObject {
    void foo() {
        WithClassObject.object.$instance.getValue();
        WithClassObject.object.$instance.getValue();
        WithClassObject.object.$instance.foo();
        WithClassObject.object.$instance.getValueWithGetter();
        WithClassObject.object.$instance.getVariable();
        WithClassObject.object.$instance.setVariable(0);
        WithClassObject.object.$instance.getVariableWithAccessors();
        WithClassObject.object.$instance.setVariableWithAccessors(0);
    }
}