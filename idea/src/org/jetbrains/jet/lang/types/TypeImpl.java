package org.jetbrains.jet.lang.types;

import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class TypeImpl extends VisitableTypeImpl {

    private final TypeConstructor constructor;
    private final List<TypeProjection> arguments;

    public TypeImpl(List<Attribute> attributes, TypeConstructor constructor, List<TypeProjection> arguments) {
        super(attributes);
        this.constructor = constructor;
        this.arguments = arguments;
    }

    @Override
    public TypeConstructor getConstructor() {
        return constructor;
    }

    @Override
    public List<TypeProjection> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return constructor + (arguments.isEmpty() ? "" : "<" + argumentsToString() + ">");
    }

    private StringBuilder argumentsToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Iterator<TypeProjection> iterator = arguments.iterator(); iterator.hasNext();) {
            TypeProjection argument = iterator.next();
            stringBuilder.append(argument);
            if (iterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }
        return stringBuilder;
    }

}
