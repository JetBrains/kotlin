package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.types.PropertyDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author abreslav
 */
public class AccumulatingScope extends JetScopeAdapter {
    private final Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();

    public AccumulatingScope(JetScope outerScope) {
        super(outerScope);
    }

    public void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        if (properties.put(propertyDescriptor.getName(), propertyDescriptor) != null) {
            throw new IllegalArgumentException("Duplicate property: " + propertyDescriptor.getName());
        }
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        PropertyDescriptor propertyDescriptor = properties.get(name);
        if (propertyDescriptor == null) {
            return super.getProperty(name);
        }
        return propertyDescriptor;
    }
}
