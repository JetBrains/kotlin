class ClassObject {
    void foo() {
        WithClassObject.Companion.getValue();
        WithClassObject.Companion.getValue();
        WithClassObject.Companion.foo();
        WithClassObject.Companion.getValueWithGetter();
        WithClassObject.Companion.getVariable();
        WithClassObject.Companion.setVariable(0);
        WithClassObject.Companion.getVariableWithAccessors();
        WithClassObject.Companion.setVariableWithAccessors(0);
    }
}