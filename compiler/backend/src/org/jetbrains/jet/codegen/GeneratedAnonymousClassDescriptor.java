/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;

import java.util.ArrayList;
import java.util.List;

public class GeneratedAnonymousClassDescriptor {
    private final JvmClassName classname;
    private final Method constructor;

    private final ClassDescriptor captureThis;
    private final ClassifierDescriptor captureReceiver;
    private final List<StackValue> args = new ArrayList<StackValue>();

    public GeneratedAnonymousClassDescriptor(
            JvmClassName classname,
            Method constructor,
            ClassDescriptor captureThis,
            ClassifierDescriptor captureReceiver
    ) {
        this.classname = classname;
        this.constructor = constructor;
        this.captureThis = captureThis;
        this.captureReceiver = captureReceiver;
    }

    public JvmClassName getClassname() {
        return classname;
    }

    public Method getConstructor() {
        return constructor;
    }

    public void addArg(StackValue local) {
        args.add(local);
    }

    public List<StackValue> getArgs() {
        return args;
    }

    public boolean isCaptureThis() {
        return captureThis != null;
    }

    public boolean isCaptureReceiver() {
        return captureReceiver != null;
    }

    public ClassDescriptor getCaptureThis() {
        return captureThis;
    }

    public ClassifierDescriptor getCaptureReceiver() {
        return captureReceiver;
    }
}
