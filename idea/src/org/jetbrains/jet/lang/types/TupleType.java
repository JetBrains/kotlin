package org.jetbrains.jet.lang.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TupleType extends TypeImpl {

    public static final int TUPLE_COUNT = 22;
    private static final TypeConstructor[] TUPLE = new TypeConstructor[TUPLE_COUNT];

    public static final TupleType UNIT = new TupleType(Collections.<Annotation>emptyList(), Collections.<Type>emptyList());

    static {
        for (int i = 0; i < TUPLE_COUNT; i++) {
            List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>();
            for (int j = 0; j < i; j++) {
                parameters.add(new TypeParameterDescriptor(
                        Collections.<Annotation>emptyList(),
                        "T" + j,
                        Variance.OUT_VARIANCE,
                        Collections.<Type>emptySet()));
            }
            TUPLE[i] = new TypeConstructor(
                    Collections.<Annotation>emptyList(),
                    "Tuple" + i,
                    parameters,
                    Collections.singleton(JetStandardTypes.getAny()));
        }
    }

    public static TupleType getTupleType(List<Annotation> annotations, List<Type> arguments) {
        if (annotations.isEmpty() && arguments.isEmpty()) {
            return UNIT;
        }
        return new TupleType(annotations, arguments);
    }


    public static TupleType getTupleType(List<Type> arguments) {
        return getTupleType(Collections.<Annotation>emptyList(), arguments);
    }

    public static TupleType getLabeledTupleType(List<Annotation> annotations, List<ParameterDescriptor> arguments) {
        return getTupleType(annotations, toTypes(arguments));
    }


    public static TupleType getLabeledTupleType(List<ParameterDescriptor> arguments) {
        return getLabeledTupleType(Collections.<Annotation>emptyList(), arguments);
    }


    private TupleType(List<Annotation> annotations, List<Type> arguments) {
        super(annotations, TUPLE[arguments.size()], toProjections(arguments));
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new UnsupportedOperationException("Not implemented"); // TODO
    }

    private static List<TypeProjection> toProjections(List<Type> arguments) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (Type argument : arguments) {
            result.add(new TypeProjection(Variance.OUT_VARIANCE, argument));
        }
        return result;
    }

    private static List<Type> toTypes(List<ParameterDescriptor> labeledEntries) {
        List<Type> result = new ArrayList<Type>();
        for (ParameterDescriptor entry : labeledEntries) {
            result.add(entry.getType());
        }
        return result;
    }
}
