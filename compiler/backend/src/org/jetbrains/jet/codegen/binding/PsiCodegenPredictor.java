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

package org.jetbrains.jet.codegen.binding;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassFqName;

public final class PsiCodegenPredictor {
    private PsiCodegenPredictor() {
    }

    public static boolean checkPredictedNameFromPsi(
            @NotNull BindingTrace bindingTrace, @NotNull DeclarationDescriptor descriptor, @Nullable Type nameFromDescriptors
    ) {
        PsiElement element = descriptorToDeclaration(bindingTrace.getBindingContext(), descriptor);
        if (element instanceof JetDeclaration) {
            String classNameFromPsi = getPredefinedJvmInternalName((JetDeclaration) element);
            assert classNameFromPsi == null || Type.getObjectType(classNameFromPsi).equals(nameFromDescriptors) :
                    String.format("Invalid algorithm for getting qualified name from psi! Predicted: %s, actual %s\n" +
                                  "Element: %s", classNameFromPsi, nameFromDescriptors, element.getText());
        }

        return true;
    }

    /**
     * TODO: Finish this method for all cases. Now it's only used and tested in JetLightClass.
     *
     * @return null if no prediction can be done.
     */
    @Nullable
    public static String getPredefinedJvmInternalName(@NotNull JetDeclaration declaration) {
        // TODO: Method won't work for declarations inside class objects
        // TODO: Method won't give correct class name for traits implementations

        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(declaration, JetDeclaration.class);
        if (parentDeclaration instanceof JetClassObject) {
            assert declaration instanceof JetObjectDeclaration : "Only object declarations can be children of JetClassObject: " + declaration;
            return getPredefinedJvmInternalName(parentDeclaration);
        }

        String parentInternalName;
        if (parentDeclaration != null) {
            parentInternalName = getPredefinedJvmInternalName(parentDeclaration);
            if (parentInternalName == null) {
                return null;
            }
        }
        else {
            String packageName = ((JetFile) declaration.getContainingFile()).getPackageName();
            if (packageName == null) {
                return null;
            }

            if (declaration instanceof JetNamedFunction) {
                JvmClassName packageClass = JvmClassName.byFqNameWithoutInnerClasses(getPackageClassFqName(new FqName(packageName)));
                Name name = ((JetNamedFunction) declaration).getNameAsName();
                return name == null ? null : packageClass.getInternalName() + "$" + name.asString();
            }

            parentInternalName = JvmClassName.byFqNameWithoutInnerClasses(packageName).getInternalName();
        }

        if (declaration instanceof JetClassObject) {
            // Get parent and assign Class object prefix
            return parentInternalName + JvmAbi.CLASS_OBJECT_SUFFIX;
        }

        if (!PsiTreeUtil.instanceOf(declaration, JetClass.class, JetObjectDeclaration.class, JetNamedFunction.class, JetProperty.class) ||
                declaration instanceof JetEnumEntry) {
            // Other subclasses are not valid for class name prediction.
            // For example EnumEntry, JetFunctionLiteral
            return null;
        }

        Name name = ((JetNamedDeclaration) declaration).getNameAsName();
        if (name == null) {
            return null;
        }

        if (declaration instanceof JetNamedFunction) {
            if (!(parentDeclaration instanceof JetClass || parentDeclaration instanceof JetObjectDeclaration)) {
                // Can't generate predefined name for internal functions
                return null;
            }
        }

        // NOTE: looks like a bug - for class in getter of top level property class name will be $propertyName$ClassName but not
        // namespace$propertyName$ClassName
        if (declaration instanceof JetProperty) {
            return parentInternalName + "$" + name.asString();
        }

        if (parentInternalName.isEmpty()) {
            return name.asString();
        }

        return parentInternalName + (parentDeclaration == null ? "/" : "$") + name.asString();
    }

    @Nullable
    public static JetFile getFileForNamespacePartName(@NotNull Collection<JetFile> allNamespaceFiles, @NotNull JvmClassName className) {
        for (JetFile file : allNamespaceFiles) {
            String internalName = NamespaceCodegen.getNamespacePartInternalName(file);
            JvmClassName jvmClassName = JvmClassName.byInternalName(internalName);
            if (jvmClassName.equals(className)) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    public static JetFile getFileForCodegenNamedClass(
            @NotNull BindingContext context,
            @NotNull Collection<JetFile> allNamespaceFiles,
            @NotNull final String classInternalName
    ) {
        final Ref<DeclarationDescriptor> resultingDescriptor = Ref.create();

        DelegatingBindingTrace trace = new DelegatingBindingTrace(context, "trace in PsiCodegenPredictor") {
            @Override
            public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
                super.record(slice, key, value);
                if (slice == CodegenBinding.ASM_TYPE && key instanceof DeclarationDescriptor && value instanceof Type) {
                    if (classInternalName.equals(((Type) value).getInternalName())) {
                        resultingDescriptor.set((DeclarationDescriptor) key);
                    }
                }
            }
        };

        CodegenBinding.initTrace(trace, allNamespaceFiles);

        return resultingDescriptor.isNull() ? null
               : BindingContextUtils.getContainingFile(trace.getBindingContext(), resultingDescriptor.get());
    }
}
