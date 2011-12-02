package org.jetbrains.jet.codegen.intrinsics;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.plugin.JetFileType;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * @author yole
 */
public class IntrinsicMethods {
    private static final IntrinsicMethod UNARY_MINUS = new UnaryMinus();
    private static final IntrinsicMethod NUMBER_CAST = new NumberCast();
    private static final IntrinsicMethod INV = new Inv();
    private static final IntrinsicMethod TYPEINFO = new TypeInfo();
    private static final IntrinsicMethod VALUE_TYPEINFO = new ValueTypeInfo();
    private static final IntrinsicMethod RANGE_TO = new RangeTo();
    private static final IntrinsicMethod INC = new Increment(1);
    private static final IntrinsicMethod DEC = new Increment(-1);

    private static final List<String> PRIMITIVE_NUMBER_TYPES = ImmutableList.of("Boolean", "Byte", "Char", "Short", "Int", "Float", "Long", "Double");
    public static final IntrinsicMethod ARRAY_SIZE = new ArraySize();
    public static final IntrinsicMethod ARRAY_INDICES = new ArrayIndices();
    public static final Equals EQUALS = new Equals();
    public static final IteratorNext ITERATOR_NEXT = new IteratorNext();
    public static final ArraySet ARRAY_SET = new ArraySet();
    public static final ArrayGet ARRAY_GET = new ArrayGet();

    private final Project myProject;
    private final JetStandardLibrary myStdLib;
    private final Map<DeclarationDescriptor, IntrinsicMethod> myMethods = new HashMap<DeclarationDescriptor, IntrinsicMethod>();
    private static final IntrinsicMethod ARRAY_ITERATOR = new ArrayIterator();

    public IntrinsicMethods(Project project, JetStandardLibrary stdlib) {
        myProject = project;
        myStdLib = stdlib;
        List<String> primitiveCastMethods = ImmutableList.of("dbl", "flt", "lng", "int", "chr", "sht", "byt");
        for (String method : primitiveCastMethods) {
            declareIntrinsicProperty("Number", method, NUMBER_CAST);
        }

        for (String type : PRIMITIVE_NUMBER_TYPES) {
            declareIntrinsicFunction(type, "minus", 0, UNARY_MINUS);
            declareIntrinsicFunction(type, "inv", 0, INV);
            declareIntrinsicFunction(type, "rangeTo", 1, RANGE_TO);
            declareIntrinsicFunction(type, "inc", 0, INC);
            declareIntrinsicFunction(type, "dec", 0, DEC);
        }

        final Set<FunctionDescriptor> typeInfoFunctionGroup = stdlib.getTypeInfoFunctions();
        declareOverload(typeInfoFunctionGroup, 0, TYPEINFO);
        declareOverload(typeInfoFunctionGroup, 1, VALUE_TYPEINFO);

        declareBinaryOp("plus", Opcodes.IADD);
        declareBinaryOp("minus", Opcodes.ISUB);
        declareBinaryOp("times", Opcodes.IMUL);
        declareBinaryOp("div", Opcodes.IDIV);
        declareBinaryOp("mod", Opcodes.IREM);
        declareBinaryOp("shl", Opcodes.ISHL);
        declareBinaryOp("shr", Opcodes.ISHR);
        declareBinaryOp("ushr", Opcodes.IUSHR);
        declareBinaryOp("and", Opcodes.IAND);
        declareBinaryOp("or", Opcodes.IOR);
        declareBinaryOp("xor", Opcodes.IXOR);

        declareIntrinsicFunction("Boolean", "not", 0, new Not());

        declareIntrinsicFunction("String", "plus", 1, new Concat());
        declareIntrinsicFunction("String", "get", 1, new StringGetChar());

        declareOverload(myStdLib.getLibraryScope().getFunctions("toString"), 0, new ToString());
        declareOverload(myStdLib.getLibraryScope().getFunctions("equals"), 1, EQUALS);
        declareOverload(myStdLib.getLibraryScope().getFunctions("identityEquals"), 1, EQUALS);
        declareOverload(myStdLib.getLibraryScope().getFunctions("plus"), 1, new StringPlus());
        declareOverload(myStdLib.getLibraryScope().getFunctions("Array"), 1, new NewArray());
        declareOverload(myStdLib.getLibraryScope().getFunctions("sure"), 0, new Sure());

        declareIntrinsicFunction("ByteIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("ShortIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("IntIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("LongIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("CharIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("BooleanIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("FloatIterator", "next", 0, ITERATOR_NEXT);
        declareIntrinsicFunction("DoubleIterator", "next", 0, ITERATOR_NEXT);

//        declareIntrinsicFunction("Any", "equals", 1, new Equals());
//
        declareIntrinsicStringMethods();
        declareIntrinsicProperty("String", "length", new StringLength());

        declareArrayMethods();
    }

    private void declareArrayMethods() {
        declareIntrinsicProperty("Array", "size", ARRAY_SIZE);
        declareIntrinsicProperty("ByteArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("ShortArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("IntArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("LongArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("FloatArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("DoubleArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("CharArray", "size", ARRAY_SIZE);
        declareIntrinsicProperty("BooleanArray", "size", ARRAY_SIZE);

        declareIntrinsicProperty("Array", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("ByteArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("ShortArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("IntArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("LongArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("FloatArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("DoubleArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("CharArray", "indices", ARRAY_INDICES);
        declareIntrinsicProperty("BooleanArray", "indices", ARRAY_INDICES);

        declareIntrinsicFunction("Array", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("ByteArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("ShortArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("IntArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("LongArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("FloatArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("DoubleArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("CharArray", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("BooleanArray", "set", 2, ARRAY_SET);

        declareIntrinsicFunction("Array", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("ByteArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("ShortArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("IntArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("LongArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("FloatArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("DoubleArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("CharArray", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("BooleanArray", "get", 1, ARRAY_GET);

        declareIterator(myStdLib.getArray());
        declareIterator(myStdLib.getByteArrayClass());
        declareIterator(myStdLib.getShortArrayClass());
        declareIterator(myStdLib.getIntArrayClass());
        declareIterator(myStdLib.getLongArrayClass());
        declareIterator(myStdLib.getFloatArrayClass());
        declareIterator(myStdLib.getDoubleArrayClass());
        declareIterator(myStdLib.getCharArrayClass());
        declareIterator(myStdLib.getBooleanArrayClass());
    }

    private void declareIterator(ClassDescriptor classDescriptor) {
        declareOverload(classDescriptor.getDefaultType().getMemberScope().getFunctions("iterator"), 0, ARRAY_ITERATOR);
    }

    private void declareIntrinsicStringMethods() {
        final ClassDescriptor stringClass = myStdLib.getString();
        final Collection<DeclarationDescriptor> stringMembers = stringClass.getMemberScope(Collections.<TypeProjection>emptyList()).getAllDescriptors();
        final PsiClass stringPsiClass = JavaPsiFacade.getInstance(myProject).findClass(
                "java.lang.String",
                new DelegatingGlobalSearchScope(ProjectScope.getLibrariesScope(myProject)) {
                    @Override
                    public boolean contains(VirtualFile file) {
                        return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
                    }
                }
        );
        for (DeclarationDescriptor stringMember : stringMembers) {
            if (stringMember instanceof FunctionDescriptor) {
                final FunctionDescriptor stringMethod = (FunctionDescriptor) stringMember;
                final PsiMethod[] methods = stringPsiClass != null?
                                            stringPsiClass.findMethodsByName(stringMember.getName(), false) : new PsiMethod[]{};
                for (PsiMethod method : methods) {
                    if (method.getParameterList().getParametersCount() == stringMethod.getValueParameters().size()) {
                        myMethods.put(stringMethod, new PsiMethodCall(stringMethod));
                    }
                }
            }
        }
    }

    private void declareBinaryOp(String methodName, int opcode) {
        BinaryOp op = new BinaryOp(opcode);
        for (String type : PRIMITIVE_NUMBER_TYPES) {
            declareIntrinsicFunction(type, methodName, 1, op);
        }
    }

    private void declareIntrinsicProperty(String className, String methodName, IntrinsicMethod implementation) {
        final JetScope numberScope = getClassMemberScope(className);
        final VariableDescriptor variable = numberScope.getVariable(methodName);
        myMethods.put(variable.getOriginal(), implementation);
    }

    private void declareIntrinsicFunction(String className, String functionName, int arity, IntrinsicMethod implementation) {
        JetScope memberScope = getClassMemberScope(className);
        final Set<FunctionDescriptor> group = memberScope.getFunctions(functionName);
        for (FunctionDescriptor descriptor : group) {
            if (className.equals(descriptor.getContainingDeclaration().getName()) && descriptor.getValueParameters().size() == arity) {
                myMethods.put(descriptor.getOriginal(), implementation);
            }
        }
    }

    private void declareOverload(Set<FunctionDescriptor> group, int arity, IntrinsicMethod implementation) {
        for (FunctionDescriptor descriptor : group) {
            if (descriptor.getValueParameters().size() == arity) {
                myMethods.put(descriptor.getOriginal(), implementation);
            }
        }
    }

    private JetScope getClassMemberScope(String className) {
        final ClassDescriptor descriptor = (ClassDescriptor) myStdLib.getLibraryScope().getClassifier(className);
        final List<TypeParameterDescriptor> typeParameterDescriptors = descriptor.getTypeConstructor().getParameters();
        List<TypeProjection> typeParameters = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            typeParameters.add(new TypeProjection(JetStandardClasses.getAnyType()));
        }
        return descriptor.getMemberScope(typeParameters);
    }

    public IntrinsicMethod getIntrinsic(DeclarationDescriptor descriptor) {
        return myMethods.get(descriptor.getOriginal());
    }

}
