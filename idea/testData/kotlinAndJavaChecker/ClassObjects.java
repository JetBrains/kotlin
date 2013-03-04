class ClassObject {
    void foo() {
        WithClassObject.object$.getValue();
        WithClassObject.object$.getValue();
        WithClassObject.object$.foo();
        WithClassObject.object$.getValueWithGetter();
        WithClassObject.object$.getVariable();
        WithClassObject.object$.setVariable(0);
        WithClassObject.object$.getVariableWithAccessors();
        WithClassObject.object$.setVariableWithAccessors(0);
    }
}