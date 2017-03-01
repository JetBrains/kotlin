// FILE: FormFieldValidatorPresenterTest.java
public class FormFieldValidatorPresenterTest<V extends String> {

    public void setValidationListenerTest(ValidationListenerTest validationListener) {
    }

    public interface ValidationListenerTest {
        void onValidityChanged(boolean valid);
    }
}
// FILE: main.kt
fun <P : FormFieldValidatorPresenterTest<String>> setValidationListener(
        presenter: P,
        validationListener: (Boolean) -> Unit
) {
    presenter.setValidationListenerTest(validationListener) // Error: Type mismatch: inferred type is (Boolean) -> Unit but FormFieldValidatorPresenterTest.ValidationListenerTest! was expected
}
