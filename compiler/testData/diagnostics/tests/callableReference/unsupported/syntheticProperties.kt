// !LANGUAGE: -ReferencesToSyntheticJavaProperties
// FIR_IDENTICAL

// FILE: Customer.java
public class Customer {
    private String name;

    public Customer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

// FILE: test.kt
val customerName = Customer::<!UNSUPPORTED_FEATURE!>name<!>
