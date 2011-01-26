package org.jetbrains.jet.types;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeCheckerTest extends LightDaemonAnalyzerTestCase {

    public static final ClassDescriptor BASE_T = new ClassDescriptor(
            Collections.<Annotation>emptyList(),
            "Base_T",
            Arrays.asList(
                new TypeParameterDescriptor(
                        Collections.<Annotation>emptyList(),
                        Variance.INVARIANT, "T",
                        Collections.<Type>emptyList())
            ),
                Collections.singleton(JetStandardClasses.getAnyType())
        );

    private static Map<String, ClassDescriptor> CLASSES = new HashMap<String, ClassDescriptor>();
    private static String[] CLASS_DECLARATIONS = {
        "class Base_T<T>",
        "class Base_inT<in T>",
        "class Base_outT<out T>",
    };

    private static final JetScope BASIC_SCOPE = new JetScope.JetScopeImpl() {
        @Override
        public ClassDescriptor getClass(String name) {
            if ("Int".equals(name)) {
                return JetStandardClasses.getInt();
            } else if ("Boolean".equals(name)) {
                return JetStandardClasses.getBoolean();
            } else if ("Byte".equals(name)) {
                return JetStandardClasses.getByte();
            } else if ("Char".equals(name)) {
                return JetStandardClasses.getChar();
            } else if ("Short".equals(name)) {
                return JetStandardClasses.getShort();
            } else if ("Long".equals(name)) {
                return JetStandardClasses.getLong();
            } else if ("Float".equals(name)) {
                return JetStandardClasses.getFloat();
            } else if ("Double".equals(name)) {
                return JetStandardClasses.getDouble();
            } else if ("Unit".equals(name)) {
                return JetStandardClasses.getTuple(0);
            } else if ("Any".equals(name)) {
                return JetStandardClasses.getAny();
            }
            if (CLASSES.isEmpty()) {
                for (String classDeclaration : CLASS_DECLARATIONS) {
                    JetClass classElement = JetChangeUtil.createClass(getProject(), classDeclaration);
                    ClassDescriptor classDescriptor = toClassDescriptor(classElement);
                    CLASSES.put(classDescriptor.getName(), classDescriptor);
                }
            }
            ClassDescriptor classDescriptor = CLASSES.get(name);
            if (classDescriptor != null) {
                return classDescriptor;
            }
            fail("Type not found: " + name);
            throw new IllegalStateException();
        }
    };


    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testConstants() throws Exception {
        assertType("1", JetStandardTypes.getInt());
        assertType("0x1", JetStandardTypes.getInt());
        assertType("0X1", JetStandardTypes.getInt());
        assertType("0b1", JetStandardTypes.getInt());
        assertType("0B1", JetStandardTypes.getInt());

        assertType("1l", JetStandardTypes.getLong());
        assertType("1L", JetStandardTypes.getLong());

        assertType("1.0", JetStandardTypes.getDouble());
        assertType("1.0d", JetStandardTypes.getDouble());
        assertType("1.0D", JetStandardTypes.getDouble());
        assertType("0x1.fffffffffffffp1023", JetStandardTypes.getDouble());

        assertType("1.0f", JetStandardTypes.getFloat());
        assertType("1.0F", JetStandardTypes.getFloat());
        assertType("0x1.fffffffffffffp1023f", JetStandardTypes.getFloat());

        assertType("true", JetStandardTypes.getBoolean());
        assertType("false", JetStandardTypes.getBoolean());

        assertType("'d'", JetStandardTypes.getChar());

        assertType("\"d\"", JetStandardTypes.getString());
        assertType("\"\"\"d\"\"\"", JetStandardTypes.getString());

        assertType("()", JetStandardTypes.getUnit());
    }

    public void testBasicSubtyping() throws Exception {
        assertSubtype("Boolean", "Boolean");
        assertSubtype("Byte", "Byte");
        assertSubtype("Char", "Char");
        assertSubtype("Short", "Short");
        assertSubtype("Int", "Int");
        assertSubtype("Long", "Long");
        assertSubtype("Float", "Float");
        assertSubtype("Double", "Double");
        assertSubtype("Unit", "Unit");
        assertSubtype("Unit", "()");
        assertSubtype("()", "Unit");
        assertSubtype("()", "()");

        assertSubtype("Boolean", "Any");
        assertSubtype("Byte", "Any");
        assertSubtype("Char", "Any");
        assertSubtype("Short", "Any");
        assertSubtype("Int", "Any");
        assertSubtype("Long", "Any");
        assertSubtype("Float", "Any");
        assertSubtype("Double", "Any");
        assertSubtype("Unit", "Any");
        assertSubtype("Any", "Any");

        assertNotSubtype("Boolean", "Byte");
        assertNotSubtype("Byte", "Short");
        assertNotSubtype("Char", "Int");
        assertNotSubtype("Short", "Int");
        assertNotSubtype("Int", "Long");
        assertNotSubtype("Long", "Double");
        assertNotSubtype("Float", "Double");
        assertNotSubtype("Double", "Int");
        assertNotSubtype("Unit", "Int");

        assertSubtype("(Boolean)", "(Boolean)");
        assertSubtype("(Byte)",    "(Byte)");
        assertSubtype("(Char)",    "(Char)");
        assertSubtype("(Short)",   "(Short)");
        assertSubtype("(Int)",     "(Int)");
        assertSubtype("(Long)",    "(Long)");
        assertSubtype("(Float)",   "(Float)");
        assertSubtype("(Double)",  "(Double)");
        assertSubtype("(Unit)",    "(Unit)");
        assertSubtype("(Unit, Unit)",    "(Unit, Unit)");

        assertSubtype("(Boolean)", "(Boolean)");
        assertSubtype("(Byte)",    "(Byte)");
        assertSubtype("(Char)",    "(Char)");
        assertSubtype("(Short)",   "(Short)");
        assertSubtype("(Int)",     "(Int)");
        assertSubtype("(Long)",    "(Long)");
        assertSubtype("(Float)",   "(Float)");
        assertSubtype("(Double)",  "(Double)");
        assertSubtype("(Unit)",    "(Unit)");
        assertSubtype("(Unit, Unit)",    "(Unit, Unit)");

        assertNotSubtype("(Unit)", "(Int)");

        assertSubtype("(Unit)",    "(Any)");
        assertSubtype("(Unit, Unit)",    "(Any, Any)");
        assertSubtype("(Unit, Unit)",    "(Any, Unit)");
        assertSubtype("(Unit, Unit)",    "(Unit, Any)");
    }

    public void testProjections() throws Exception {
        assertSubtype("Base_T<Int>", "Base_T<Int>");
    }

    public void testImplicitConversions() throws Exception {
        assertConvertibleTo("1", JetStandardTypes.getByte());
    }

    private static ClassDescriptor toClassDescriptor(JetClass classElement) {
        return new ClassDescriptor(
                toAttributes(classElement.getModifierList()),
                classElement.getName(),
                toTypeParameters(classElement.getTypeParameters()),
                toTypes(classElement.getDelegationSpecifiers())
                );
    }

    private static List<TypeParameterDescriptor> toTypeParameters(List<JetTypeParameter> typeParameters) {
        List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
        for (JetTypeParameter typeParameter : typeParameters) {
            result.add(toTypeParameter(typeParameter));
        }
        return result;
    }

    private static TypeParameterDescriptor toTypeParameter(JetTypeParameter typeParameter) {
        JetTypeReference extendsBound = typeParameter.getExtendsBound();
        return new TypeParameterDescriptor(
            toAttributes(typeParameter.getModifierList()),
            typeParameter.getVariance(),
            typeParameter.getName(),
            extendsBound == null ? Collections.<Type>singleton(JetStandardClasses.getAnyType()) : Collections.singleton(toType(extendsBound))
        );
    }

    private static Collection<? extends Type> toTypes(List<JetDelegationSpecifier> delegationSpecifiers) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        throw new UnsupportedOperationException(); // TODO
    }

    private static void assertSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, true);
    }

    private static void assertNotSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, false);
    }

    private static void assertSubtypingRelation(String type1, String type2, boolean expected) {
        Type typeNode1 = toType(JetChangeUtil.createType(getProject(), type1));
        Type typeNode2 = toType(JetChangeUtil.createType(getProject(), type2));
        boolean result = new JetTypeChecker().isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertTrue(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, result == expected);
    }

    private static Type toType(JetTypeReference typeNode) {
        List<JetAttribute> attributeElements = typeNode.getAttributes();
        final List<Annotation> attributes = toAttributes(attributeElements);
        JetTypeElement typeElement = typeNode.getTypeElement();

        // TODO annotations
        final Type[] result = new Type[1];
        typeElement.accept(new JetVisitor() {
            @Override
            public void visitUserType(JetUserType type) {
                List<JetTypeProjection> argumentElements = type.getTypeArguments();

                final List<TypeProjection> arguments = toTypeProjections(argumentElements);
                result[0] = new ClassType(attributes, TypeResolver.INSTANCE.resolveClass(BASIC_SCOPE, type), arguments);
            }

            @Override
            public void visitTupleType(JetTupleType type) {
                // TODO labels
                result[0] = TupleType.getTupleType(toTypes(type.getComponentTypeRefs()));
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported type: " + elem);
            }
        });

        return result[0];
    }

    private static List<Annotation> toAttributes(List<JetAttribute> attributeElements) {
        return Collections.emptyList();
    }

    private static List<Annotation> toAttributes(JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        // TODO:
        return Collections.emptyList();
    }

    private static List<Type> toTypes(List<JetTypeReference> argumentElements) {
        final List<Type> arguments = new ArrayList<Type>();
        for (JetTypeReference argumentElement : argumentElements) {
            arguments.add(toType(argumentElement));
        }
        return arguments;
    }

    private static List<TypeProjection> toTypeProjections(List<JetTypeProjection> argumentElements) {
        final List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        for (JetTypeProjection argumentElement : argumentElements) {
            Type type = toType(argumentElement.getTypeReference());
            TypeProjection typeProjection = new TypeProjection(argumentElement.getProjectionKind(), type);
            arguments.add(typeProjection);
        }
        return arguments;
    }

    private static void assertConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertTrue(
                expression + " must be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private static void assertNotConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertFalse(
                expression + " must not be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private static void assertType(String expression, Type expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        Type type = new JetTypeChecker().getType(jetExpression);
        assertEquals(type, expectedType);
    }
}
