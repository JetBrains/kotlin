package org.jetbrains.jet.codegen.intrinsics;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.TypeProjection;
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

    private static final List<String> PRIMITIVE_NUMBER_TYPES = ImmutableList.of("Boolean", "Byte", "Char", "Short", "Int", "Float", "Long", "Double");

    private final Project myProject;
    private final JetStandardLibrary myStdLib;
    private final Map<DeclarationDescriptor, IntrinsicMethod> myMethods = new HashMap<DeclarationDescriptor, IntrinsicMethod>();

    public IntrinsicMethods(Project project, JetStandardLibrary stdlib) {
        myProject = project;
        myStdLib = stdlib;
        List<String> primitiveCastMethods = ImmutableList.of("dbl", "flt", "lng", "int", "chr", "sht", "byt");
        for (String method : primitiveCastMethods) {
            declareIntrinsicProperty("Number", method, NUMBER_CAST);
        }
        declareIntrinsicProperty("Array", "size", new ArraySize());

        for (String type : PRIMITIVE_NUMBER_TYPES) {
            declareIntrinsicFunction(type, "minus", 0, UNARY_MINUS);
            declareIntrinsicFunction(type, "inv", 0, INV);
            declareIntrinsicFunction(type, "rangeTo", 1, RANGE_TO);
        }

        final FunctionGroup typeInfoFunctionGroup = stdlib.getTypeInfoFunctionGroup();
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
        declareIntrinsicFunction("String", "plusAssign", 1, new PlusConcat());

        declareIntrinsicStringMethods();
    }

    private void declareIntrinsicStringMethods() {
        final ClassDescriptor stringClass = myStdLib.getString();
        final Collection<DeclarationDescriptor> stringMembers = stringClass.getMemberScope(Collections.<TypeProjection>emptyList()).getAllDescriptors();
        final PsiClass stringPsiClass = JavaPsiFacade.getInstance(myProject).findClass("java.lang.String",
                                                                                       ProjectScope.getLibrariesScope(myProject));
        for (DeclarationDescriptor stringMember : stringMembers) {
            if (stringMember instanceof FunctionDescriptor) {
                final FunctionDescriptor stringMethod = (FunctionDescriptor) stringMember;
                final PsiMethod[] methods = stringPsiClass.findMethodsByName(stringMember.getName(), false);
                for (PsiMethod method : methods) {
                    if (method.getParameterList().getParametersCount() == stringMethod.getValueParameters().size()) {
                        myMethods.put(stringMethod, new PsiMethodCall(method));
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
        final FunctionGroup group = memberScope.getFunctionGroup(functionName);
        declareOverload(group, arity, implementation);
    }

    private void declareOverload(FunctionGroup group, int arity, IntrinsicMethod implementation) {
        for (FunctionDescriptor descriptor : group.getFunctionDescriptors()) {
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
