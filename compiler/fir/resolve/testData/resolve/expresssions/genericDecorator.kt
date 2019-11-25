// FILE: LookupElement.java

public abstract class LookupElement {
    public abstract String getLookupString();
}

// FILE: Decorator.java

public abstract class Decorator<T extends LookupElement> extends LookupElement {
    public T getDelegate() {
        return null;
    }
}

// FILE: test.kt

class MyDecorator : Decorator<LookupElement> {
    override fun getLookupString(): String = delegate.lookupString
}