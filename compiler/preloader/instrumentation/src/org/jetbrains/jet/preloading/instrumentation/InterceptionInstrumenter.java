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

import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.util.Textifier;
import org.jetbrains.asm4.util.TraceMethodVisitor;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.asm4.Opcodes.*;

public class InterceptionInstrumenter {
    private static final Pattern ANYTHING = Pattern.compile(".*");

    private final boolean dumpInstrumentedMethods = false;

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
        for (Field field : handlerClass.getFields()) {
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

                Class<?> interceptorClass = interceptor.getClass();

                FieldData fieldData = getFieldData(field, interceptorClass);

                List<MethodData> enterData = new ArrayList<MethodData>();
                List<MethodData> exitData = new ArrayList<MethodData>();
                List<Method> dumpMethods = new ArrayList<Method>();
                for (Method method : interceptorClass.getMethods()) {
                    String name = method.getName();
                    if (name.startsWith("enter")) {
                        enterData.add(getMethodData(fieldData, method));
                    }
                    else if (name.startsWith("exit")) {
                        exitData.add(getMethodData(fieldData, method));
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

                String nameFromAnnotation = annotation.methodName();
                String methodName = nameFromAnnotation.isEmpty() ? field.getName() : nameFromAnnotation;
                MethodInstrumenterImpl instrumenter = new MethodInstrumenterImpl(
                        field.getDeclaringClass().getSimpleName() + "." + field.getName(),
                        classPattern,
                        compilePattern(methodName),
                        compilePattern(annotation.erasedSignature()),
                        annotation.allowMultipleMatches(),
                        enterData,
                        exitData,
                        annotation.logInterceptions());

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
        return new FieldDataImpl(
                            Type.getInternalName(field.getDeclaringClass()),
                            field.getName(),
                            Type.getDescriptor(field.getType()),
                            Type.getType(runtimeType));
    }

    private static MethodData getMethodData(FieldData interceptorField, Method method) {
        return new MethodDataImpl(
            interceptorField,
            Type.getInternalName(method.getDeclaringClass()),
            method.getName(),
            Type.getMethodDescriptor(method),
            method.getParameterTypes().length
        );
    }

    private void addDumpTask(final Object interceptor, final Method method, final MethodInstrumenter instrumenter) {
        dumpTasks.add(new DumpAction() {
            @Override
            public void dump(PrintStream out) {
                out.println("<<< " + instrumenter + ": " + interceptor.getClass().getCanonicalName() + " says:");
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
            }
        });
    }

    public byte[] instrument(String resourceName, byte[] data) {
        List<MethodInstrumenter> applicableInstrumenters = new ArrayList<MethodInstrumenter>();
        String className = stripClassSuffix(resourceName);
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

    private static String stripClassSuffix(String name) {
        String suffix = ".class";
        if (!name.endsWith(suffix)) return name;
        return name.substring(0, name.length() - suffix.length());
    }

    private byte[] instrument(byte[] classData, final List<MethodInstrumenter> instrumenters) {
        final ClassReader cr = new ClassReader(classData);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(ASM4, cw) {
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

                final List<MethodData> exitData = new ArrayList<MethodData>();
                final List<MethodData> enterData = new ArrayList<MethodData>();
                for (MethodInstrumenter instrumenter : applicableInstrumenters) {
                    enterData.addAll(instrumenter.getEnterData());
                    exitData.addAll(instrumenter.getExitData());
                }

                if (enterData.isEmpty() && exitData.isEmpty()) return mv;

                if (dumpInstrumentedMethods) {
                    mv = getDumpingVisitorWrapper(mv, name, desc);
                }

                return new MethodVisitor(ASM4, mv) {

                    private InstructionAdapter ia = null;

                    private InstructionAdapter getInstructionAdapter() {
                        if (ia == null) {
                            ia = new InstructionAdapter(this);
                        }
                        return ia;
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        int sizes = Type.getArgumentsAndReturnSizes(desc);
                        super.visitMaxs(Math.max(maxStack, sizes + 1), maxLocals);
                    }

                    @Override
                    public void visitCode() {
                        for (MethodData methodData : enterData) {
                            invokeMethod(access, name, desc, getInstructionAdapter(), methodData);
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
                            case ATHROW:
                                for (MethodData methodData : exitData) {
                                    invokeMethod(access, name, desc, getInstructionAdapter(), methodData);
                                }
                                break;
                        }
                        super.visitInsn(opcode);
                    }
                };
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

            private TraceMethodVisitor getDumpingVisitorWrapper(MethodVisitor mv, final String name, final String desc) {
                return new TraceMethodVisitor(mv, new Textifier() {
                    @Override
                    public void visitMethodEnd() {
                        System.out.println(cr.getClassName() + ":" + name + desc);
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

    private static void invokeMethod(int access, String name, String desc, InstructionAdapter ia, MethodData methodData) {
        FieldData field = methodData.getOwnerField();
        ia.getstatic(field.getDeclaringClass(), field.getName(), field.getDesc());
        ia.checkcast(field.getRuntimeType());

        int parameterCount = methodData.getParameterCount();
        if (parameterCount > 0) {
            org.jetbrains.asm4.commons.Method method = new org.jetbrains.asm4.commons.Method(name, desc);
            Type[] parameterTypes = method.getArgumentTypes();
            int base = (access & ACC_STATIC) != 0 ? 0 : 1;
            for (int i = 0; i < parameterCount; i++) {
                ia.load(base + i, parameterTypes[i]);
            }
        }
        ia.invokevirtual(methodData.getDeclaringClass(), methodData.getName(), methodData.getDesc());
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
