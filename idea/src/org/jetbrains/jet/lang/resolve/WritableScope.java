package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author abreslav
 */
public class WritableScope implements JetScope {
    private Map<String, PropertyDescriptor> propertyDescriptors;

    public void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        if (propertyDescriptors == null) {
            propertyDescriptors = new HashMap<String, PropertyDescriptor>();
        }
        assert !propertyDescriptors.containsKey(propertyDescriptor.getName()) : "Property redeclared";
        propertyDescriptors.put(propertyDescriptor.getName(), propertyDescriptor);
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        return propertyDescriptors.get(name);
    }

    @NotNull
    @Override
    public Collection<MethodDescriptor> getMethods(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ClassDescriptor getClass(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeParameterDescriptor getTypeParameterDescriptor(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }
}
