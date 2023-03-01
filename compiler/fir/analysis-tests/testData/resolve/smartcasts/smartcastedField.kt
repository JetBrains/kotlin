// ISSUE: KT-56820

// FILE: PropertyId.java
public class PropertyId<T> {}

// FILE: ReferenceId.java
public class ReferenceId extends PropertyId<Object> {}

// FILE: Property.java
public class Property<T> {
    public PropertyId<T> id = new PropertyId<>();
}

// FILE: Reference.java
public class Reference extends Property<Object> {
    public ReferenceId getId() { return new ReferenceId(); }
}

// FILE: main.kt
private val anyProperty = Property<Any>()
private val boundedProperty = Property<String>()

fun test_1(x: Property<Any>) {
    if (x is Reference) {
        x.id
    }
}

fun test_2(x: Property<String>) {
    if (x is Reference) {
        x.id
    }
}
