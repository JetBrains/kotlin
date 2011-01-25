package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class TypeVisitor<R, D> {
    public R visitNothingType(NothingType nothingType, D data) {
        return visitType(nothingType, data);
    }

    public R visitTupleType(TupleType tupleType, D data) {
        return visitType(tupleType, data);
    }

    public R visitTypeVariable(TypeVariable typeVariable, D data) {
        return visitType(typeVariable, data);
    }

    public R visitClassType(ClassType classType, D data) {
        return visitType(classType, data);
    }

    public R visitThisType(ThisType thisType, D data) {
        return visitType(thisType, data);
    }
    
    public R visitType(Type type, D data) {
        throw new UnsupportedOperationException(); // TODO
    }
}
