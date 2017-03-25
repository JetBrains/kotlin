/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.debugger.evaluate;

import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Copied from com.intellij.debugger.ui.impl.watch.CompilingEvaluator
public class CompilingEvaluatorUtils {
    public static ClassLoaderReference getClassLoader(EvaluationContext context, DebugProcess process)
            throws EvaluateException, InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        // TODO: cache
        ClassType loaderClass = (ClassType)process.findClass(context, "java.net.URLClassLoader", context.getClassLoader());
        Method ctorMethod = loaderClass.concreteMethodByName("<init>", "([Ljava/net/URL;Ljava/lang/ClassLoader;)V");
        ClassLoaderReference reference = (ClassLoaderReference)process.newInstance(context, loaderClass, ctorMethod, Arrays.asList(createURLArray(context), context.getClassLoader()));
        keep(reference, context);
        return reference;
    }

    public static void keep(ObjectReference reference, EvaluationContext context) {
        ((SuspendContextImpl)context.getSuspendContext()).keep(reference);
    }

    public static byte[] changeSuperToMagicAccessor(byte[] bytes) {
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if ("java/lang/Object".equals(superName)) {
                    superName = "sun/reflect/MagicAccessorImpl";
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
        new ClassReader(bytes).accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

    public static ArrayReference mirrorOf(byte[] bytes, EvaluationContext context, DebugProcess process)
            throws EvaluateException, InvalidTypeException, ClassNotLoadedException {
        ArrayType arrayClass = (ArrayType)process.findClass(context, "byte[]", context.getClassLoader());
        ArrayReference reference = process.newInstance(arrayClass, bytes.length);
        keep(reference, context);
        for (int i = 0; i < bytes.length; i++) {
            reference.setValue(i, ((VirtualMachineProxyImpl)process.getVirtualMachineProxy()).mirrorOf(bytes[i]));
        }
        return reference;
    }

    public static void defineClass(
            @NotNull String className,
            byte[] bytecodes,
            @NotNull EvaluationContext context,
            @NotNull DebugProcess process,
            @NotNull ClassLoaderReference classLoader
    ) throws ClassNotLoadedException, EvaluateException, InvalidTypeException {
        VirtualMachineProxyImpl proxy = (VirtualMachineProxyImpl)process.getVirtualMachineProxy();

        Method defineMethod =
                ((ClassType)classLoader.referenceType()).concreteMethodByName("defineClass", "(Ljava/lang/String;[BII)Ljava/lang/Class;");
        byte[] bytes = changeSuperToMagicAccessor(bytecodes);
        ArrayList<Value> args = new ArrayList<Value>();
        StringReference name = proxy.mirrorOf(className);
        keep(name, context);
        args.add(name);
        args.add(mirrorOf(bytes, context, process));
        args.add(proxy.mirrorOf(0));
        args.add(proxy.mirrorOf(bytes.length));
        process.invokeMethod(context, classLoader, defineMethod, args);
    }

    private static ArrayReference createURLArray(EvaluationContext context)
            throws EvaluateException, InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        DebugProcess process = context.getDebugProcess();
        ArrayType arrayType = (ArrayType)process.findClass(context, "java.net.URL[]", context.getClassLoader());
        ArrayReference arrayRef = arrayType.newInstance(1);
        keep(arrayRef, context);
        ClassType classType = (ClassType)process.findClass(context, "java.net.URL", context.getClassLoader());
        VirtualMachineProxyImpl proxy = (VirtualMachineProxyImpl)process.getVirtualMachineProxy();
        StringReference url = proxy.mirrorOf("file:a");
        keep(url, context);
        ObjectReference reference = process.newInstance(context, classType, classType.concreteMethodByName("<init>", "(Ljava/lang/String;)V"),
                                                        Collections.singletonList(url));
        keep(reference, context);
        arrayRef.setValues(Collections.singletonList(reference));
        return arrayRef;
    }
}
