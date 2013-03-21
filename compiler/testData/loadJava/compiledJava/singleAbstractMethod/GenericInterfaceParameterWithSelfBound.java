package test;

import java.util.List;

public interface GenericInterfaceParameterWithSelfBound<T extends GenericInterfaceParameterWithSelfBound<T>> {
    T method(T t);
}
