class ClassObject {
    void foo() {
        WithClassObject.OBJECT$.getValue();
        WithClassObject.OBJECT$.getValue();
        WithClassObject.OBJECT$.foo();
        WithClassObject.OBJECT$.getValueWithGetter();
        WithClassObject.OBJECT$.getVariable();
        WithClassObject.OBJECT$.setVariable(0);
        WithClassObject.OBJECT$.getVariableWithAccessors();
        WithClassObject.OBJECT$.setVariableWithAccessors(0);
    }
}