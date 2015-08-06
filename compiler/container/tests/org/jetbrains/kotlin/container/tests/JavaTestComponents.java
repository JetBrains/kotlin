package org.jetbrains.kotlin.container.tests;

public class JavaTestComponents {
    public final Iterable<TestClientComponentInterface> components;
    public final Iterable<TestGenericComponent<String>> genericComponents;

    public JavaTestComponents(Iterable<TestClientComponentInterface> components,
            Iterable<TestGenericComponent<String>> genericComponents) {
        this.components = components;
        this.genericComponents = genericComponents;
    }
}
