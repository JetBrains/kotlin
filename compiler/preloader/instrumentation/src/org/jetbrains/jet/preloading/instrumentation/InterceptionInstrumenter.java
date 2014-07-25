/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.preloading.instrumentation;

import org.jetbrains.jet.preloading.instrumentation.annotations.*;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class InterceptionInstrumenter {
    private static final Pattern ANYTHING = Pattern.compile(".*");
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    private final Map<String, ClassMatcher> classPatterns = new LinkedHashMap<String, ClassMatcher>();

    private final Set<String> neverMatchedClassPatterns = new LinkedHashSet<String>();
    private final Set<MethodInstrumenter> neverMatchedInstrumenters = new LinkedHashSet<MethodInstrumenter>();

    interface DumpAction {
        void dump(PrintStream out);
    }
    private final List<DumpAction> dumpTasks = new ArrayList<DumpAction>();

    public InterceptionInstrumenter(List<Class<?>> handlerClasses) {
        for (Class<?> handlerClass : handlerClasses) {
            addHandlerClass(handlerClass);
        }
    }

    private void addHandlerClass(Class<?> handlerClass) {
        for (final Field field : handlerClass.getFields()) {
            MethodInterceptor annotation = field.getAnnotation(MethodInterceptor.class);
            if (annotation == null) continue;

            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                throw new IllegalArgumentException("Non-static field annotated @MethodInterceptor: " + field);
            }

            Pattern classPattern = compilePattern(annotation.className());
            List<MethodInstrumenter> instrumenters = addClassPattern(classPattern);

            try {
                Object interceptor = field.get(null);
                if (interceptor == null) {
                    throw new IllegalArgumentException("Interceptor is null: " + field);
                }

                final Class<?> interceptorClass = interceptor.getClass();

                FieldData fieldData = getFieldData(field, interceptorClass);

                List<MethodData> enterData = new ArrayList<MethodData>();
                List<MethodData> normalReturnData = new ArrayList<MethodData>();
                List<MethodData> exceptionData = new ArrayList<MethodData>();
                List<Method> dumpMethods = new ArrayList<Method>();
                for (Method method : interceptorClass.getMethods()) {
                    String name = method.getName();
                    MethodData methodData = getMethodData(fieldData, method);
                    if (name.startsWith("enter")) {
                        enterData.add(methodData);
                    }
                    else if (name.startsWith("normalReturn")) {
                        normalReturnData.add(methodData);
                    }
                    else if (name.startsWith("exception")) {
                        exceptionData.add(methodData);
                    }
                    else if (name.startsWith("exit")) {
                        normalReturnData.add(methodData);
                        exceptionData.add(methodData);
                    }
                    else if (name.startsWith("dump")) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        // Dump must have no parameters or one PrintStream parameter
                        if (parameterTypes.length > 1) continue;
                        if (parameterTypes.length == 1 && parameterTypes[0] != PrintStream.class) {
                            continue;
                        }
                        dumpMethods.add(method);
                    }
                }

                if (enterData.isEmpty() && normalReturnData.isEmpty() && exceptionData.isEmpty()) {
                    dumpTasks.add(new DumpAction() {
                        @Override
                        public void dump(PrintStream out) {
                            out.println("WARNING: No relevant methods found in " + field + " of type " + interceptorClass.getCanonicalName());
                        }
                    });
                }

                String nameFromAnnotation = annotation.methodName();
                String methodName = nameFromAnnotation.isEmpty() ? field.getName() : nameFromAnnotation;
                MethodInstrumenter instrumenter = new MethodInstrumenter(
                        field.getDeclaringClass().getSimpleName() + "." + field.getName(),
                        classPattern,
                        compilePattern(methodName),
                        compilePattern(annotation.methodDesc()),
                        annotation.allowMultipleMatches(),
                        enterData,
                        normalReturnData,
                        exceptionData,
                        annotation.logInterceptions(),
                        annotation.dumpByteCode());

                for (Method dumpMethod : dumpMethods) {
                    addDumpTask(interceptor, dumpMethod, instrumenter);
                }

                instrumenters.add(instrumenter);
                neverMatchedInstrumenters.add(instrumenter);
            }
            catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }

        }
    }

    private List<MethodInstrumenter> addClassPattern(Pattern classPattern) {
        ClassMatcher classMatcher = classPatterns.get(classPattern.pattern());
        if (classMatcher == null) {
            classMatcher = new ClassMatcher(classPattern);
            classPatterns.put(classPattern.pattern(), classMatcher);
            neverMatchedClassPatterns.add(classPattern.pattern());
        }
        return classMatcher.instrumenters;
    }

    private static FieldData getFieldData(Field field, Class<?> runtimeType) {
        return new FieldData(
                            Type.getInternalName(field.getDeclaringClass()),
                            field.getName(),
                            Type.getDescriptor(field.getType()),
                            Type.getType(runtimeType));
    }

    private static MethodData getMethodData(FieldData interceptorField, Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int thisParameterIndex = -1;
        int classNameParameterIndex = -1;
        int methodNameParameterIndex = -1;
        int methodDescParameterIndex = -1;
        int allArgsParameterIndex = -1;
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof This) {
                    if (thisParameterIndex > -1) {
                        throw new IllegalArgumentException("Multiple @This parameters in " + method);
                    }
                    thisParameterIndex = i;
                }
                else if (annotation instanceof ClassName) {
                    if (classNameParameterIndex > -1) {
                        throw new IllegalArgumentException("Multiple @ClassName parameters in " + method);
                    }
                    classNameParameterIndex = i;
                }
                else if (annotation instanceof MethodName) {
                    if (methodNameParameterIndex > -1) {
                        throw new IllegalArgumentException("Multiple @MethodName parameters in " + method);
                    }
                    methodNameParameterIndex = i;
                }
                else if (annotation instanceof MethodDesc) {
                    if (methodDescParameterIndex > -1) {
                        throw new IllegalArgumentException("Multiple @MethodDesc parameters in " + method);
                    }
                    methodDescParameterIndex = i;
                }
                else if (annotation instanceof AllArgs) {
                    if (allArgsParameterIndex > -1) {
                        throw new IllegalArgumentException("Multiple @AllArgs parameters in " + method);
                    }
                    allArgsParameterIndex = i;
                }
            }
        }
        return new MethodData(
            interceptorField,
            Type.getInternalName(method.getDeclaringClass()),
            method.getName(),
            Type.getMethodDescriptor(method),
            thisParameterIndex,
            classNameParameterIndex,
            methodNameParameterIndex,
            methodDescParameterIndex,
            allArgsParameterIndex);
    }

    private void addDumpTask(final Object interceptor, final Method method, final MethodInstrumenter instrumenter) {
        dumpTasks.add(new DumpAction() {
            @Override
            public void dump(PrintStream out) {
                out.println("<<< " + instrumenter + ": " + interceptor.getClass().getName() + " says:");
                try {
                    if (method.getParameterTypes().length == 0) {
                        method.invoke(interceptor);
                    }
                    else {
                        method.invoke(interceptor, out);
                    }
                }
                catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                catch (InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
                out.println(">>>");
                out.println();
            }
        });
    }

    public byte[] instrument(String resourceName, byte[] data) {
        try {
            String className = stripClassSuffix(resourceName);
            if (className == null) {
                // Not a .class file
                return data;
            }

            List<MethodInstrumenter> applicableInstrumenters = new ArrayList<MethodInstrumenter>();
            for (Map.Entry<String, ClassMatcher> classPatternEntry : classPatterns.entrySet()) {
                String classPattern = classPatternEntry.getKey();
                ClassMatcher classMatcher = classPatternEntry.getValue();
                if (classMatcher.classPattern.matcher(className).matches()) {
                    neverMatchedClassPatterns.remove(classPattern);
                    applicableInstrumenters.addAll(classMatcher.instrumenters);
                }
            }

            if (applicableInstrumenters.isEmpty()) return data;

            return instrument(data, applicableInstrumenters);
        }
        catch (Throwable e) {
            throw new IllegalStateException("Exception while instrumenting " + resourceName, e);
        }
    }

    private static String stripClassSuffix(String name) {
        String suffix = ".class";
        if (!name.endsWith(suffix)) return null;
        return name.substring(0, name.length() - suffix.length());
    }

    private byte[] instrument(byte[] classData, final List<MethodInstrumenter> instrumenters) {
        final ClassReader cr = new ClassReader(classData);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(ASM5, cw) {
            private final Map<MethodInstrumenter, String> matchedMethods = new HashMap<MethodInstrumenter, String>();

            @Override
            public MethodVisitor visitMethod(
                    final int access,
                    final String name,
                    final String desc,
                    String signature,
                    String[] exceptions
            ) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                // Do not instrument synthetic methods
                if ((access & (ACC_BRIDGE | ACC_SYNTHETIC)) != 0) return mv;

                List<MethodInstrumenter> applicableInstrumenters = new ArrayList<MethodInstrumenter>();
                for (MethodInstrumenter instrumenter : instrumenters) {
                    if (instrumenter.isApplicable(name, desc)) {
                        applicableInstrumenters.add(instrumenter);
                        instrumenter.reportApplication(cr.getClassName(), name, desc);

                        checkMultipleMatches(instrumenter, name, desc);
                        neverMatchedInstrumenters.remove(instrumenter);
                    }
                }

                if (applicableInstrumenters.isEmpty()) return mv;

                boolean dumpByteCode = false;
                final List<MethodData> normalReturnData = new ArrayList<MethodData>();
                final List<MethodData> enterData = new ArrayList<MethodData>();
                final List<MethodData> exceptionData = new ArrayList<MethodData>();
                for (MethodInstrumenter instrumenter : applicableInstrumenters) {
                    enterData.addAll(instrumenter.getEnterData());

                    normalReturnData.addAll(instrumenter.getNormalReturnData());

                    exceptionData.addAll(instrumenter.getExceptionData());

                    dumpByteCode |= instrumenter.shouldDumpByteCode();
                }

                if (enterData.isEmpty() && normalReturnData.isEmpty() && exceptionData.isEmpty()) return mv;

                if (dumpByteCode) {
                    mv = getDumpingVisitorWrapper(mv, name, desc);
                }

                final int maxStackDepth = getMaxStackDepth(name, desc, normalReturnData, enterData, exceptionData);
                final boolean isConstructor = "<init>".equals(name);

                return new MethodVisitor(ASM5, mv) {

                    private InstructionAdapter ia = null;

                    private InstructionAdapter getInstructionAdapter() {
                        if (ia == null) {
                            ia = new InstructionAdapter(this);
                        }
                        return ia;
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(Math.max(maxStack, maxStackDepth), maxLocals);
                    }

                    @Override
                    public void visitCode() {
                        for (MethodData methodData : enterData) {
                            // At the very beginning of a constructor, i.e. before any super() call, 'this' is not available
                            // It's too hard to detect a place right after the super() call, so we just put null instead of 'this' in such cases
                            invokeMethod(access, cr.getClassName(), name, desc, getInstructionAdapter(), methodData, isConstructor);
                        }
                        super.visitCode();
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        switch (opcode) {
                            case RETURN:
                            case IRETURN:
                            case LRETURN:
                            case FRETURN:
                            case DRETURN:
                            case ARETURN:
                                for (MethodData methodData : normalReturnData) {
                                    invokeMethod(access, cr.getClassName(), name, desc, getInstructionAdapter(), methodData, false);
                                }
                                break;
                            case ATHROW:
                                for (MethodData methodData : exceptionData) {
                                    // A constructor may throw before calling super(), 'this' is not available in this case
                                    invokeMethod(access, cr.getClassName(), name, desc, getInstructionAdapter(), methodData, isConstructor);
                                }
                                break;
                        }
                        super.visitInsn(opcode);
                    }
                };
            }

            private int getMaxStackDepth(
                    String name,
                    String desc,
                    List<MethodData> normalReturnData,
                    List<MethodData> enterData,
                    List<MethodData> exceptionData
            ) {
                org.jetbrains.org.objectweb.asm.commons.Method methodBeingInstrumented = new org.jetbrains.org.objectweb.asm.commons.Method(name, desc);

                List<MethodData> allData = new ArrayList<MethodData>();
                allData.addAll(enterData);
                allData.addAll(exceptionData);
                allData.addAll(normalReturnData);
                int maxStackDepth = 0;
                for (MethodData methodData : allData) {
                    int depth = stackDepth(methodData, methodBeingInstrumented);
                    if (maxStackDepth < depth) {
                        maxStackDepth = depth;
                    }
                }
                return maxStackDepth;
            }

            private int stackDepth(MethodData methodData, org.jetbrains.org.objectweb.asm.commons.Method methodBeingInstrumented) {
                org.jetbrains.org.objectweb.asm.commons.Method method = getAsmMethod(methodData);

                // array * 2 (dup) + index + value (may be long/double)
                int allArgsStackDepth = methodData.getAllArgsParameterIndex() >= 0 ? 5 : 0;

                int argsSize = 0;
                for (Type type : method.getArgumentTypes()) {
                    argsSize += type.getSize();
                }

                int receiverSize = 1;
                // return value must be kept on the stack OR exception, so we have to reserve at least 1
                int exceptionSize = 1;
                int returnValueSize = methodBeingInstrumented.getReturnType().getSize();
                return argsSize + allArgsStackDepth + receiverSize + Math.max(returnValueSize, exceptionSize);
            }

            private void checkMultipleMatches(MethodInstrumenter instrumenter, String name, String desc) {
                if (!instrumenter.allowsMultipleMatches()) {
                    String erasedSignature = name + desc;
                    String alreadyMatched = matchedMethods.put(instrumenter, erasedSignature);
                    if (alreadyMatched != null) {
                        throw new IllegalStateException(instrumenter + " matched two methods in " + cr.getClassName() + ":\n"
                                                        + alreadyMatched + "\n"
                                                        + erasedSignature);
                    }
                }
            }

            private TraceMethodVisitor getDumpingVisitorWrapper(MethodVisitor mv, final String methodName, final String methodDesc) {
                return new TraceMethodVisitor(mv, new Textifier() {
                    @Override
                    public void visitMethodEnd() {
                        System.out.println(cr.getClassName() + ":" + methodName + methodDesc);
                        for (Object line : getText()) {
                            System.out.print(line);
                        }
                        System.out.println();
                        System.out.println();
                        super.visitMethodEnd();
                    }
                });
            }
        }, 0);
        return cw.toByteArray();
    }

    private static org.jetbrains.org.objectweb.asm.commons.Method getAsmMethod(MethodData methodData) {
        return new org.jetbrains.org.objectweb.asm.commons.Method(methodData.getName(), methodData.getDesc());
    }

    private static void invokeMethod(
            int instrumentedMethodAccess,
            String instrumentedClassName,
            String instrumentedMethodName,
            String instrumentedMethodDesc,
            InstructionAdapter ia,
            MethodData methodData,
            boolean thisUnavailable
    ) {
        FieldData field = methodData.getOwnerField();
        ia.getstatic(field.getDeclaringClass(), field.getName(), field.getDesc());
        ia.checkcast(Type.getObjectType(methodData.getDeclaringClass()));

        org.jetbrains.org.objectweb.asm.commons.Method asmMethod = getAsmMethod(methodData);

        Type[] interceptingMethodParameterTypes = asmMethod.getArgumentTypes();
        int parameterCount = interceptingMethodParameterTypes.length;
        if (parameterCount > 0) {
            Type[] instrumentedMethodParameterTypes = Type.getArgumentTypes(instrumentedMethodDesc);
            boolean isStatic = (instrumentedMethodAccess & ACC_STATIC) != 0;
            int base = isStatic ? 0 : 1;
            int instrumentedMethodParameterIndex = 0;
            int instrumentedMethodParameterOffset = 0;
            for (int i = 0; i < parameterCount; i++) {
                if (i == methodData.getThisParameterIndex()) {
                    if (isStatic || thisUnavailable) {
                        // a) static method, 'this' is null
                        // b) this is not available (some locations in constructors
                        ia.aconst(null);
                    }
                    else {
                        // load 'this'
                        ia.load(0, OBJECT_TYPE);
                    }
                }
                else if (i == methodData.getClassNameParameterIndex()) {
                    ia.aconst(instrumentedClassName);
                }
                else if (i == methodData.getMethodNameParameterIndex()) {
                    ia.aconst(instrumentedMethodName);
                }
                else if (i == methodData.getMethodDescParameterIndex()) {
                    ia.aconst(instrumentedMethodDesc);
                }
                else if (i == methodData.getAllArgsParameterIndex()) {
                    ia.aconst(instrumentedMethodParameterTypes.length);
                    ia.newarray(OBJECT_TYPE);
                    int offset = 0;
                    for (int parameterIndex = 0; parameterIndex < instrumentedMethodParameterTypes.length; parameterIndex++) {
                        ia.dup();

                        ia.iconst(parameterIndex);

                        Type type = instrumentedMethodParameterTypes[parameterIndex];
                        ia.load(base + offset, type);
                        offset += type.getSize();

                        boxOrCastIfNeeded(ia, type, OBJECT_TYPE);
                        ia.astore(OBJECT_TYPE);
                    }
                }
                else {
                    Type type = instrumentedMethodParameterTypes[instrumentedMethodParameterIndex];
                    ia.load(base + instrumentedMethodParameterOffset, type);
                    boxOrCastIfNeeded(ia, type, interceptingMethodParameterTypes[i]);

                    instrumentedMethodParameterIndex++;
                    instrumentedMethodParameterOffset += type.getSize();
                }

            }
        }
        ia.invokevirtual(methodData.getDeclaringClass(), methodData.getName(), methodData.getDesc());
        Type type = asmMethod.getReturnType();
        if (type.getSort() != Type.VOID) {
            if (type.getSize() == 1) {
                ia.pop();
            }
            else {
                ia.pop2();
            }
        }
    }

    private static void boxOrCastIfNeeded(InstructionAdapter ia, Type from, Type to) {
        if (isPrimitive(to)) {
            if (!isPrimitive(from)) {
                throw new IllegalArgumentException("Cannot cast " + from + " to " + to);
            }
            ia.cast(from, to);
            return;
        }
        switch (from.getSort()) {
            case Type.BOOLEAN:
                box(ia, from, Boolean.class);
                break;
            case Type.CHAR:
                box(ia, from, Character.class);
                break;
            case Type.BYTE:
                box(ia, from, Byte.class);
                break;
            case Type.SHORT:
                box(ia, from, Short.class);
                break;
            case Type.INT:
                box(ia, from, Integer.class);
                break;
            case Type.FLOAT:
                box(ia, from, Float.class);
                break;
            case Type.LONG:
                box(ia, from, Long.class);
                break;
            case Type.DOUBLE:
                box(ia, from, Double.class);
                break;
            default:
                // Nothing to do, it's an object already
        }
    }

    private static boolean isPrimitive(Type to) {
        return to.getSort() <= Type.DOUBLE;
    }

    private static void box(InstructionAdapter ia, Type from, Class<?> boxedClass) {
        Type boxedType = Type.getType(boxedClass);
        ia.invokestatic(boxedType.getInternalName(), "valueOf", "(" + from.getDescriptor() + ")" + boxedType.getDescriptor(), false);
    }

    public void dump(PrintStream out) {
        for (DumpAction task : dumpTasks) {
            task.dump(out);
        }

        if (!neverMatchedClassPatterns.isEmpty()) {
            out.println("Class patterns never matched:");
            for (String classPattern : neverMatchedClassPatterns) {
                out.println("    " + classPattern);
                neverMatchedInstrumenters.removeAll(classPatterns.get(classPattern).instrumenters);
            }
        }

        if (!neverMatchedInstrumenters.isEmpty()) {
            out.println("Instrumenters never matched: ");
            for (MethodInstrumenter instrumenter : neverMatchedInstrumenters) {
                out.println("    " + instrumenter);
            }
        }
    }

    private static Pattern compilePattern(String regex) {
        if (regex.isEmpty()) return ANYTHING;
        return Pattern.compile(regex);
    }

    private static class ClassMatcher {
        private final Pattern classPattern;
        private final List<MethodInstrumenter> instrumenters = new ArrayList<MethodInstrumenter>();

        private ClassMatcher(Pattern classPattern) {
            this.classPattern = classPattern;
        }
    }
}
